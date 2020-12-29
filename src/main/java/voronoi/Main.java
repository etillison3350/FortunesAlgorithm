package voronoi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Application;

import voronoi.render.Window;

public class Main {

    public static void main(final String[] args) {
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(final int b) throws IOException {}
        }));
//        System.setErr(new PrintStream(new OutputStream() {
//            @Override
//            public void write(final int b) throws IOException {}
//        }));

        Application.launch(Window.class, args);
    }

}
