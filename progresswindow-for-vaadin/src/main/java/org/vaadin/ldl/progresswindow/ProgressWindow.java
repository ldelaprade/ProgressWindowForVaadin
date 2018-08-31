/**
 *
 * @author laurent_de_laprade@hotmail.com
 *
 * -------------------------------------------------------------
 * Usage examples:
 * -------------------------------------------------------------
 * Usage 1 : a Progress windows which is not cancel-able
 * -------------------------------------------------------------
 *      new JetProgressWindow("Loading Table")
 *      {
 *          private static final long serialVersionUID = 1L;
 *          @Override
 *          public void lengthyOperation() throws Exception
 *          {
 *              loadCategories(treeCategories);
 *          }
 *      };
 * -------------------------------------------------------------
 * Usage 2 : a Progress windows with a Cancel Button
 * -------------------------------------------------------------
 *      new JetProgressWindow("Connecting you...", "Please wait", "Can't wait")
 *      {
 *          private static final long serialVersionUID = 1L;
 *          @Override
 *          public void lengthyOperation() throws Exception
 *          {
 *              ProcessLogin();
 *          }
 *      };
 *
 */

package org.vaadin.ldl.progresswindow;

import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

//
//    PROGRESS WINDOW DESIGN
//    drawing using http://asciiflow.com/
//
//    +---------------------------------------------------------------------------+
//    |                                                                           |
//    |  Progress window title                                                    |  CAPTION ZONE
//    |                                                                           |
//    +---------------------------------------------------------------------------+
//    +---------------------------------------------------------------------------+
//    |                                                                           |  DESCRIPTION LABEL
//    |    Progess infos ...                                                      |
//    |                                                                           |
//    |                                                                           |
//    |                                                                           |
//    |                                                                           |
//    +-----------------------+---------------------------+-----------------------+
//    |                       |                           |                       |
//    |                       |  progress Animation       |                       |  PROGRESS BAR
//    |                       |                           |                       |
//    |                       +---------------------------+  +------------------+ |
//    |                                                      |                  | |
//    |                                                      | Optional Cancel  | |  CANCEL BUTTON
//    |                                                      | button           | |
//    |                                                      +------------------+ |
//    +---------------------------------------------------------------------------+
//
//
public abstract class ProgressWindow extends Window implements FocusListener, ClickListener
{
    private static final long serialVersionUID = 5253966202431615380L;
    private VerticalLayout layout = new VerticalLayout();
    private Label description = new Label();
    private ProgressBar progressIndicator = new ProgressBar();

    // The lengthy action you'll implement
    public abstract void lengthyOperation() throws Exception;
    private boolean processWasStarted = false;

    // members for a 'Cancel' button mode
    // We won't use semaphore and thread spawning for
    // the lightweight non-cancel-able progress windows mode
    private boolean hasCancelButton = false;
    private NativeButton btnKill = null;
    String btnKillCaption = null;
    Thread threadedLenghtyOperation = null;

    public ProgressWindow(String caption)
    {
        init(caption, "Please wait...");
    }
    public ProgressWindow()
    {
        init("Job in progress", "Please wait...");
    }
    public ProgressWindow(String caption, String progressInfo)
    {
        init(caption, progressInfo);
    }

    public ProgressWindow(String caption, String progressInfo, String killBtnText)
    {
        if( UI.getCurrent().getPushConfiguration().getPushMode().isEnabled() )
        {
          hasCancelButton = true;
          btnKillCaption = killBtnText;
        }
        //else
        //    Notification.show("Vaadin Push not enabled. 'Cancel' button cannot be added", Type.HUMANIZED_MESSAGE);
        init(caption, progressInfo);
    }

    public void init(String caption, String progressInfo)
    {
        //WebBrowser webBrowser = Page.getCurrent().getWebBrowser();

        setCaption(caption);
        setClosable(false);

        center();

        layout.addComponent(description);
        description.setHeight("40px");
        layout.setComponentAlignment(description, Alignment.MIDDLE_CENTER);
        description.setValue(progressInfo);
        layout.addComponent(progressIndicator);
        layout.setComponentAlignment(progressIndicator, Alignment.MIDDLE_CENTER);
        progressIndicator.setIndeterminate(true);

        setContent(layout);
        layout.setMargin(true);
        setWidth("500px");
        setHeight("200px");
        setResizable(false);
        UI.getCurrent().addWindow(this);

        if( hasCancelButton && btnKill==null )
        {
            btnKill = new NativeButton();
            btnKill.setCaption(btnKillCaption);
            layout.addComponent(btnKill);
            layout.setComponentAlignment(btnKill, Alignment.BOTTOM_CENTER);

            this.setClosable(true);
            btnKill.addClickListener(clicEvent -> buttonClick(clicEvent));
        }

        // SetModal call will force focus on us,
        // then, focus will cause the lengthy operation to start
        setModal(true);

        // e.g. in autotest mode, we cannot relay on getting focus
        Object urlAttribute = VaadinSession.getCurrent().getAttribute("NoProgressWindow");
        if ( urlAttribute!=null && (boolean)urlAttribute )
        {
            // Direct processing - Progress Window will not be visible
            try
            {
                lengthyOperation();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally
            {
                cleanExit();
            }
        }
        else
        {
            // The only way to make Progress Window visible before lengthy process is started:
            // Start the Modal progress window; when it gets focus, ask server to launch lengthy job
            addFocusListener(this);
        }

    }

    //@SuppressWarnings("deprecation")
    @Override
    public void buttonClick(ClickEvent event)
    {
        if( btnKill!=null && event.getButton() == btnKill )
        {
            btnKill.setEnabled(false);
            btnKill.setCaption("Closing...");

            if( threadedLenghtyOperation!=null )
            {
                threadedLenghtyOperation.interrupt();
            }
            setModal(false);
            close();
        }
    }

    @Override
    public void focus(FocusEvent event)
    {
        // processWasStarted flag to protect against multiple 'OnFocus' events
        if( !processWasStarted )
        {
            try
            {
                processWasStarted = true;
                final UI appUI = UI.getCurrent();

                if( !hasCancelButton || appUI == null ) lengthyOperation();
                else
                {
                    threadedLenghtyOperation = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                lengthyOperation();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                cleanExit();
                                // Need push call because we are in Background thread
                                // To enable Push, Note that UI class need Push annotation like this:
                                // @Push(PushMode.MANUAL)
                                // public class JetUI extends UI
                                appUI.push();
                            }
                        }
                    };

                    threadedLenghtyOperation.start();

                }

            }
            catch (Exception e)
            {
                OnException(e);
            }
            finally
            {
                if( btnKill==null )
                {
                    // When no Task thread used,
                    // When we are here, task is complete
                    cleanExit();
                }
            }
        }
    }


    protected void cleanExit()
    {
        setModal(false);
        close();
    }

    // can be overriden to cheeck what happened
    protected void OnException(Exception e)
    {
        e.printStackTrace();
    }

}
