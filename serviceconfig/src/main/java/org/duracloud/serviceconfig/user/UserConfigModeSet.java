/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.serviceconfig.user;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrew Woods
 *         Date: Aug 23, 2010
 */
public class UserConfigModeSet implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id = -1;
    private String name;
    private String displayName;
    private String value;
    private List<UserConfigMode> modes;

    public UserConfigModeSet() {
    }

    public UserConfigModeSet(List<UserConfig> userConfigs) {
        UserConfigMode mode = new UserConfigMode();
        mode.setSelected(true);
        mode.setName("defaultMode");
        mode.setDisplayName("Default Mode");
        mode.setUserConfigs(userConfigs);
        this.displayName = "Default Mode Set";
        this.name = "defaultModeSet";
        this.value = mode.getName();
        this.modes = Arrays.asList(new UserConfigMode[]{mode});
    }

    public boolean hasOnlyUserConfigs() {
        if (null != modes) {
            if (modes.size() == 1) {
                UserConfigMode mode = modes.get(0);
                if (null == mode.getUserConfigModeSets()) {
                    return (null != mode.getUserConfigs() &&
                        mode.getUserConfigs().size() > 0);
                }
            }
        }
        return false;
    }

    public List<UserConfig> wrappedUserConfigs() {
        if (!hasOnlyUserConfigs()) {
            throw new RuntimeException("Not a UserConfigs wrapper object.");
        }
        return modes.get(0).getUserConfigs();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<UserConfigMode> getModes() {
        return modes;
    }

    public void setModes(List<UserConfigMode> modes) {
        this.modes = modes;
    }
}
