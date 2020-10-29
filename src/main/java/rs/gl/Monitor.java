package rs.gl;

import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;

public class Monitor {
    final long handle;

    public Monitor(long handle) {
        this.handle = handle;
    }

    public static Monitor primaryMonitor() {
        return new Monitor(glfwGetPrimaryMonitor());
    }

    public int width() {
        return glfwGetVideoMode(handle).width();
    }

    public int height() {
        return glfwGetVideoMode(handle).height();
    }
}
