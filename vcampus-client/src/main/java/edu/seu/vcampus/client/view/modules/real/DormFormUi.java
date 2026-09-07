package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;

/** Shared layout primitives for dorm forms. */
final class DormFormUi {
    private DormFormUi() {
    }

    static JPanel field(String label, Component control, int width) {
        JPanel holder = new JPanel(new BorderLayout(0, 5));
        holder.setOpaque(false);
        holder.add(DormUi.caption(label), BorderLayout.NORTH);
        holder.add(control, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(width, 60));
        return holder;
    }

    static JPanel primarySecondaryActions(String primaryText, ActionListener primaryAction,
                                          String secondaryText, ActionListener secondaryAction) {
        JButton primary = new PrimaryButton(primaryText);
        primary.addActionListener(primaryAction);
        JButton secondary = new SecondaryButton(secondaryText);
        secondary.addActionListener(secondaryAction);
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(primary);
        buttons.add(secondary);
        return buttons;
    }
}
