package corelyzer.plugin;

import corelyzer.ui.CorelyzerApp;

import javax.media.opengl.GL;
import javax.swing.*;
import java.awt.*;

public class FreeDrawTemplate extends CorelyzerPlugin {
    CorelyzerApp app;
    JFrame pluginFrame;
    float dY = 0.0f;
    int trackID;

    public boolean init(Component parentUI) {
        app = CorelyzerApp.getApp();

        int id = getPluginID();
        app.getPluginManager().registerPluginForEventType(id,
                CorelyzerPluginEvent.TRACK_MOVED);

        this.setupUI();
        return true;
    }

    public void fini() {
        // do nothing
    }

    public void processEvent(CorelyzerPluginEvent e) {
        // TODO more flexible with plugin events
        if(e.getID() == CorelyzerPluginEvent.TRACK_MOVED) {
            String msg = e.getDescription();
            // System.out.println("---> [MESG] Track Moved: '" + msg + "'");

            String [] toks = msg.split("\t");
            trackID = Integer.valueOf(toks[0]);
            dY = Float.valueOf(toks[2]); // * SceneGraph.getCanvasDPIY(0);
            // System.out.println("---> [MESG] dY = " + dY + " in trackId: " + trackID);
        }
    }

    public JFrame getFrame() {
        return pluginFrame;
    }

    public String getMenuName() {
        return "FreeDraw Template";
    }

    public void renderRectangle(GL gl, int freeDraw, int canvas, int track,
                                int section, float x, float y, float w, float h,
                                float scale)
    {
        // determine if it's a client position free draw or a discussion thread
        // marker

        // look up client data from the freedraw -> client data table
        // to get user name

        // draw a rectangle filling white quad
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3f(1, 0, 0);
        gl.glLineWidth(1.5f);

        gl.glPushMatrix();
        {
            // if(this.trackID == track) {
            //    gl.glTranslatef(0.0f, dY, 0.0f);
            // }

            gl.glBegin(GL.GL_LINE_LOOP);
            {
                gl.glVertex3f(x  , y  , 0);
                gl.glVertex3f(x  , y+h, 0);
                gl.glVertex3f(x+w, y+h, 0);
                gl.glVertex3f(x+w, y  , 0);
            }
            gl.glEnd();
        }
        gl.glPopMatrix();

        gl.glPopAttrib();
    }

    private void setupUI() {
        pluginFrame = new FDDialog(this.getPluginID());
        pluginFrame.pack();
        pluginFrame.setLocationRelativeTo(null);

        try {
            pluginFrame.setAlwaysOnTop(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
