package edu.seu.vcampus.server.router;

import edu.seu.vcampus.common.protocol.Message;

/** 流式命令向当前 Socket 串行写回事件的边界。 */
public interface StreamWriter {
    void write(Message message);
}
