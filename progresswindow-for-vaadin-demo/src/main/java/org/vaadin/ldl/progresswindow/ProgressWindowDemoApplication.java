package org.vaadin.ldl.progresswindow;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Demo application for the <a
 * href="http://vaadin.com/addon/progresswindow-for-vaadin">Wizards for Vaadin</a>
 * add-on.
 *
 * @author Laurent de Laprade / Vaadin Ltd
 */
@SuppressWarnings("serial")
@Theme("demo")
@Push(PushMode.MANUAL)
public class ProgressWindowDemoApplication extends UI
{

    private VerticalLayout mainLayout;

    @Override
    protected void init(VaadinRequest request)
    {
        // setup the main window
        mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setMargin(true);
        setContent(mainLayout);

        // create the buttons wich triggers lengthy operations
        Button startLengthyOperation = new Button
        (
            "Start a non cancellable lengthy operation...",
            new Button.ClickListener()
            {
                @Override
                public void buttonClick(ClickEvent event)
                {
                    new ProgressWindow("Demo Progress Window", "Non Cancellable Lenghy operation")
                    {
                        @Override
                        public void lengthyOperation() throws Exception
                        {
                            // Simulate lengthy operation: sleep 10 seconds
                            Thread.sleep(10*1000);
                        }
                    };
                }
            }
        );
        mainLayout.addComponent(startLengthyOperation);
        mainLayout.setComponentAlignment(startLengthyOperation, Alignment.TOP_CENTER);

        Button startCancellableLengthyOperation = new Button
        (
            "Start a cancellable lengthy operation...",
            new Button.ClickListener()
            {
                @Override
                public void buttonClick(ClickEvent event)
                {
                    new ProgressWindow("Demo Progress Window", "Cancellable Lenghy operation", "It's too long ! I'd better give up")
                    {
                        @Override
                        public void lengthyOperation() throws Exception
                        {
                            // Simulate lengthy operation: sleep 20 seconds
                            Thread.sleep(10*1000);
                        }
                    };
                }
            }
        );
        mainLayout.addComponent(startCancellableLengthyOperation);
        mainLayout.setComponentAlignment(startCancellableLengthyOperation, Alignment.TOP_CENTER);

        Label infos = new Label("Click one of the Demo buttons above");
        mainLayout.addComponent(infos);
        mainLayout.setComponentAlignment(infos, Alignment.TOP_CENTER);
        mainLayout.setExpandRatio(infos, (float) 2.0);
    }



}
