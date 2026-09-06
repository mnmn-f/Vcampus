package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiToolStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String description;
    private final String parameterGuide;
    private final boolean writeOperation;
    private final boolean available;
    public AiToolStatus(String name, String description, String parameterGuide,
            boolean writeOperation, boolean available) {
        this.name = name; this.description = description; this.parameterGuide = parameterGuide;
        this.writeOperation = writeOperation; this.available = available;
    }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParameterGuide() { return parameterGuide; }
    public boolean isWriteOperation() { return writeOperation; }
    public boolean isAvailable() { return available; }
}
