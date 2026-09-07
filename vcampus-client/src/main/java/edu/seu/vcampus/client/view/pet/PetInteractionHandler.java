package edu.seu.vcampus.client.view.pet;

/** 桌宠安全交互扩展点；互动只改变本地动画，不发起模型或业务请求。 */
public interface PetInteractionHandler {
    void onPrimaryAction();

    void onRestoreRequested();

    void onOpenAssistantRequested();

    void onFeedRequested();

    void onPetRequested();
}
