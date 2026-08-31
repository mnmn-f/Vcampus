package edu.seu.vcampus.client.composition;

import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.service.campus.NetworkCampusClientService;
import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.service.identity.NetworkIdentityClientService;
import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.service.library.NetworkLibraryClientService;
import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.NetworkDormClientService;
import edu.seu.vcampus.client.service.store.NetworkStoreClientService;
import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.service.student.NetworkStudentRecordClientService;
import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.session.ClientSession;

/** 网络模式的客户端组合根；业务服务共享 gateway、请求边界和会话。 */
public final class ClientBusinessServices {
    private final NetworkClientService network;
    private final ClientSession session;
    private final StudentRecordClientService student;
    private final AcademicClientService academic;
    private final CampusClientService campus;
    private final IdentityClientService identity;
    private final LibraryClientService library;
    private final StoreClientService store;
    private final DormClientService dorm;

    public ClientBusinessServices(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) {
            throw new IllegalArgumentException("network services require network and session");
        }
        this.network = network;
        this.session = session;
        student = new NetworkStudentRecordClientService(network, session);
        academic = new AcademicClientService(network, session);
        campus = new NetworkCampusClientService(network, session);
        identity = new NetworkIdentityClientService(network, session);
        library = new NetworkLibraryClientService(network, session);
        store = new NetworkStoreClientService(network, session);
        dorm = new NetworkDormClientService(network, session);
    }

    public StudentRecordClientService student() { return student; }
    public AcademicClientService academic() { return academic; }
    public CampusClientService campus() { return campus; }
    public IdentityClientService identity() { return identity; }
    public LibraryClientService library() { return library; }
    public StoreClientService store() { return store; }
    public DormClientService dorm() { return dorm; }

    public void synchronizeSession() {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
    }

    public void clearSession() { network.setSessionToken(null); }
}
