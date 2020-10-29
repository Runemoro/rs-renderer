package rs.renderer;

import rs.gl.Button;
import rs.gl.ButtonEvent;
import rs.util.Color;
import rs.gl.CursorMode;
import rs.gl.GlProgram;
import rs.gl.GlUtil;
import rs.gl.GraphicsSystem;
import rs.gl.Window;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import rs.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2d;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Renderer {
    private static final double FOV = 0.5;
    public static final double CROSSHAIR_SIZE = 50;
    public static final double CROSSHAIR_THICKNESS = 5;
    public static final int CHUNK_SIZE = 8;
    public int viewDistance = 150;
    public int viewDistanceDynamic = 25;
    private static final Color FOG_COLOR = new Color(0.8, 0.9, 0.95);

    private Window window;

    private final Vector3d position = new Vector3d(3223, 3425, 20);
    private final Quaterniond rotation = new Quaterniond();
    private static final double lookSpeed = 1.7;
    private static final double rollSpeed = 0.1;
    private static double moveSpeed = 20;

    private final List<Button> buttonsPressed = new ArrayList<>();
    private boolean mouseLocked = false;
    private double lastMouseX;
    private double lastMouseY;
    private long lastInputTime;

    private GlProgram program;
    private GraphicsSystem gs;
    private final World world = new World();
    private final ChunkRenderScheduler chunkRenderer = new ChunkRenderScheduler(world);

    public void run() {
        gs = GraphicsSystem.instance();

        window = gs.window();
        window.setSamples(4);
        window.setSize(960, 720);
        window.setMaximized(true);
        window.setTitle("Renderer");
        window.setRefreshAction(this::draw);
        window.create();
        window.show();

        glEnable(GL_MULTISAMPLE);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try {
            program = new GlProgram(
                    new String(Renderer.class.getResourceAsStream("/shaders/vertex-shader.glsl").readAllBytes()),
                    new String(Renderer.class.getResourceAsStream("/shaders/fragment-shader.glsl").readAllBytes())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long time = System.nanoTime();
        int frames = 0;
        while (!window.shouldClose()) {
            if (frames++ == 100) {
                frames = 0;
                System.out.println(1 / ((double) (System.nanoTime() - time) / 1000000000 / 100) + " fps");
                time = System.nanoTime();
            }
            draw();
            handleInput();
        }

        window.destroy();
        gs.terminate();
    }

    private void draw() {
        long ss = System.nanoTime();
        int width = window.framebufferWidth();
        int height = window.framebufferHeight();
        glViewport(0, 0, width, height);
        glClearColor((float) FOG_COLOR.r, (float) FOG_COLOR.g, (float) FOG_COLOR.b, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        drawCrosshair(width, height);

        Matrix4d projection = new Matrix4d()
                .perspective(FOV * Math.PI, (double) width / height, 50 / 128., Double.POSITIVE_INFINITY);

        Matrix4d transform = new Matrix4d()
                .rotate(rotation)
                .translate(-position.x, -position.y, -position.z);

        Vector3d light = new Vector3d(position.x - 50, position.y - 50, 200);

        program.enable(
                transform.get(new float[16]),
                projection.get(new float[16]),
                new float[]{(float) light.x, (float) light.y, (float) light.z},
                viewDistance - CHUNK_SIZE,
                FOG_COLOR,
                new float[]{(float) position.x, (float) position.y, (float) position.z},
                0.7f
        );

        List<WorldRenderer> chunks = new ArrayList<>();

        for (int dx = -viewDistance; dx <= viewDistance; dx += CHUNK_SIZE) {
            int w = (int) Math.sqrt(viewDistance * viewDistance - dx * dx);

            for (int dy = -w; dy <= w; dy += CHUNK_SIZE) {
                int x = (int) (position.x + dx) / CHUNK_SIZE;
                int y = (int) (position.y + dy) / CHUNK_SIZE;


                WorldRenderer chunk = chunkRenderer.get(x, y);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        for (WorldRenderer chunk : chunks) {
            program.render(chunk.opaqueBuffer.buffer());
        }

//        long s = System.nanoTime();
//        WorldRenderer dynamicRenderer = new WorldRenderer(world);
//
//        for (int dx = -viewDistance; dx <= viewDistance; dx += CHUNK_SIZE) {
//            int w = (int) Math.sqrt(viewDistance * viewDistance - dx * dx);
//
//            for (int dy = -w; dy <= w; dy += CHUNK_SIZE) {
//                int x = (int) (position.x + dx) / CHUNK_SIZE;
//                int y = (int) (position.y + dy) / CHUNK_SIZE;
//
//                if (new Vector2d(dx, dy).length() < viewDistanceDynamic) {
//                    dynamicRenderer.dynamicLocations(x, y);
//                }
//            }
//        }
//
//        program.render(dynamicRenderer.opaqueBuffer.buffer());
//        program.render(dynamicRenderer.translucentBuffer.buffer());
//
//        long dynamicTime = System.nanoTime() - s;

        for (WorldRenderer chunk : chunks) {
            program.render(chunk.translucentBuffer.buffer());
        }

        program.disable();
        window.swapBuffers();
        GlUtil.checkError();


//        System.out.println(1. * dynamicTime / (System.nanoTime() - ss));
    }

    private void drawCrosshair(int width, int height) {
        glColor4d(0.25, 0.25, 0.25, 1);
        glBegin(GL_QUADS);
        glVertex2d(-CROSSHAIR_SIZE / 2 / width, -CROSSHAIR_THICKNESS / 2 / height);
        glVertex2d(CROSSHAIR_SIZE / 2 / width, -CROSSHAIR_THICKNESS / 2 / height);
        glVertex2d(CROSSHAIR_SIZE / 2 / width, CROSSHAIR_THICKNESS / 2 / height);
        glVertex2d(-CROSSHAIR_SIZE / 2 / width, CROSSHAIR_THICKNESS / 2 / height);

        glVertex2d(CROSSHAIR_THICKNESS / 2 / width, -CROSSHAIR_SIZE / 2 / height);
        glVertex2d(CROSSHAIR_THICKNESS / 2 / width, CROSSHAIR_SIZE / 2 / height);
        glVertex2d(-CROSSHAIR_THICKNESS / 2 / width, CROSSHAIR_SIZE / 2 / height);
        glVertex2d(-CROSSHAIR_THICKNESS / 2 / width, -CROSSHAIR_SIZE / 2 / height);
        glEnd();
    }

    private void handleInput() {
        gs.pollEvents();
        double time = Math.min((System.nanoTime() - lastInputTime) / 1000000000., 0.2);
        lastInputTime = System.nanoTime();

        window.acceptScrolls(v -> {
            if (buttonsPressed.contains(Button.LEFT_ALT)) {
                moveSpeed += v.y / 5;
                if (moveSpeed < 0.1) {
                    moveSpeed = 0.1;
                }
            } else {
                rotation.rotateLocalZ(v.y * rollSpeed);
            }
        });

        window.acceptButtonEvents(event -> {
            if (event.action == ButtonEvent.Action.PRESS) {
                buttonsPressed.add(event.button);
            }

            if (event.action == ButtonEvent.Action.RELEASE) {
                buttonsPressed.remove(event.button);

                if (event.button == Button.LEFT_MOUSE) {
                    if (!mouseLocked) {
                        lockMouse();
                    }
                }

                if (event.button == Button.ESCAPE) {
                    unlockMouse();
                }
            }
        });

        if (mouseLocked) {
            rotation.rotateLocalX((window.mouseY() - lastMouseY) * lookSpeed / window.height())
                    .rotateLocalY((window.mouseX() - lastMouseX) * lookSpeed / window.height());

            lastMouseX = window.mouseX();
            lastMouseY = window.mouseY();
        }

        double speedMultiplier = 1;
        if (buttonsPressed.contains(Button.LEFT_SHIFT)) speedMultiplier *= 5;
        if (buttonsPressed.contains(Button.LEFT_CONTROL)) speedMultiplier /= 5;

        if (buttonsPressed.contains(Button.Z)) {
            rotation.rotateLocalZ(-rollSpeed * speedMultiplier * time);
        }

        if (buttonsPressed.contains(Button.X)) {
            rotation.rotateLocalZ(rollSpeed * speedMultiplier * time);
        }

        if (buttonsPressed.contains(Button.S)) {
            move(0, 0, -moveSpeed * speedMultiplier * time);
        }

        if (buttonsPressed.contains(Button.W)) {
            move(0, 0, moveSpeed * speedMultiplier * time);
        }

        if (buttonsPressed.contains(Button.A)) {
            move(-moveSpeed * speedMultiplier * time, 0, 0);
        }

        if (buttonsPressed.contains(Button.D)) {
            move(moveSpeed * speedMultiplier * time, 0, 0);
        }

        if (buttonsPressed.contains(Button.Q)) {
            move(0, -moveSpeed * speedMultiplier * time, 0);
        }

        if (buttonsPressed.contains(Button.E)) {
            move(0, moveSpeed * speedMultiplier * time, 0);
        }
    }

    private void lockMouse() {
        if (!mouseLocked) {
            mouseLocked = true;
            window.setCursorMode(CursorMode.DISABLED);
            lastMouseX = window.mouseX();
            lastMouseY = window.mouseY();
        }
    }

    private void unlockMouse() {
        if (mouseLocked) {
            window.setCursorMode(CursorMode.NORMAL);
        }

        mouseLocked = false;
    }


    public void move(double right, double up, double forward) {
        position.add(rotation.transformInverse(new Vector3d(right, up, -forward)));
    }
}
