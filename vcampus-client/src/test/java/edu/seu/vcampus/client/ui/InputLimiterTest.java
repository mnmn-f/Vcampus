package edu.seu.vcampus.client.ui;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class InputLimiterTest {
    @Test public void rejectsInvalidCharactersAndExcessLengthWhileTyping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField mobile = new JTextField(); InputLimiter.mobile(mobile);
            mobile.setText("17740208438"); assertEquals("17740208438", mobile.getText());
            mobile.setText("177-4020"); assertEquals("17740208438", mobile.getText());

            JTextField score = new JTextField(); InputLimiter.decimal(score, 3, 2);
            score.setText("98.50"); assertEquals("98.50", score.getText());
            score.setText("98.501"); assertEquals("98.50", score.getText());

            JTextField name = new JTextField(); InputLimiter.personName(name);
            name.setText("王晨茜"); assertEquals("王晨茜", name.getText());
            name.setText("王123"); assertEquals("王晨茜", name.getText());
        });
    }
}
