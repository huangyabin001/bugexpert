package com.bill.bugexpert;

import java.net.URI;

public class PlatformUtil {

    //public static final String ASSETS_ROOT = "/";

    public static void openUri(String uri) {
        try {
            java.awt.Desktop.getDesktop().browse(URI.create(uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
