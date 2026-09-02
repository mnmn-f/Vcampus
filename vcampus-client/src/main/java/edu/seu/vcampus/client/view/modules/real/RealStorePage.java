package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 校园商店网络页面，按当前职责组合学生消费或商店管理能力。 */
public final class RealStorePage extends BasePage {
    public RealStorePage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.STORE), "");
        Role role = session.getActiveRole();
        setHeaderContext(role.getDisplayName());
        if (role == Role.STUDENT) buildStudent(services);
        else if (role == Role.STORE_MANAGER) buildManager(services);
        else warning("当前角色没有校园商店页面。");
    }

    private void buildStudent(ClientBusinessServices services) {
        TaskTabs tabs = new TaskTabs();
        final StoreOrdersPanel orders = new StoreOrdersPanel(this, services.store(), Role.STUDENT);
        tabs.addTask("选购商品", new StoreProductsPanel(this, services.store(), Role.STUDENT));
        tabs.addTask("购物车", new StoreCartPanel(this, services.store(), new Runnable() {
            @Override public void run() { orders.reload(); }
        }));
        tabs.addTask("我的订单", orders);
        tabs.addTask("校园账户", new StoreAccountPanel(this, services.store()));
        addBlock(tabs);
    }

    private void buildManager(ClientBusinessServices services) {
        TaskTabs tabs = new TaskTabs();
        tabs.addTask("商品管理", new StoreProductsPanel(this, services.store(), Role.STORE_MANAGER));
        tabs.addTask("订单处理", new StoreOrdersPanel(this, services.store(), Role.STORE_MANAGER));
        tabs.addTask("销售概览", new StoreSalesPanel(this, services.store()));
        addBlock(tabs);
    }
}
