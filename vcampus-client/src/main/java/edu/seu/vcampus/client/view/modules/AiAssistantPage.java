package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;

import javax.swing.JLabel;

/** 校园助手页面。 */
public final class AiAssistantPage extends BasePage {
    public interface Factory {
        AiAssistantClientService create();
    }

    private final AiAssistantClientService service;

    private AiAssistantPage(ClientSession session, Factory factory) {
        super(session, "校园助手", "");
        setHeaderContext("身份：" + session.getActiveRole().getDisplayName());
        AiAssistantClientService built = factory == null ? null : factory.create();
        service = built == null ? new DisabledAiAssistantClientService() : built;
        addBlock(unavailableCard());
    }

    public static AiAssistantPage create(ClientSession session, Factory factory) {
        return new AiAssistantPage(session, factory);
    }

    private SectionCard unavailableCard() {
        SectionCard card = new SectionCard("校园助手", "");
        card.setContent(new JLabel("校园助手暂不可用。", JLabel.CENTER));
        return card;
    }
}
