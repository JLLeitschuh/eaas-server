package de.bwl.bwfla.emil.datatypes;

import de.bwl.bwfla.emil.datatypes.rest.EmilContainerNetworkingType;
import de.bwl.bwfla.emil.datatypes.rest.EmilNetworkingType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class EmilContainerEnvironment extends EmilEnvironment {

    @XmlElement
    private String input;

    @XmlElement
    private String output;

    @XmlElement
    private List<String> args;

    @XmlElement
    private List<String> env;

    @XmlElement
    private String runtimeId;

    @XmlElement
    private EmilContainerNetworkingType networking;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        if (args != null) {
            this.args = new ArrayList<>();
            this.args.addAll(args);
        }
    }

    public List<String> getEnv() {
        return env;
    }

    public void setEnv(List<String> env) {
        if (env != null) {
            this.env = new ArrayList<>();
            this.env.addAll(env);
        }
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public void setRuntimeId(String runtimeId) {
        this.runtimeId = runtimeId;
    }

    @Override
    public EmilContainerNetworkingType getNetworking() {
        return this.networking;

    }

    public void setNetworking(EmilContainerNetworkingType networking) {
        this.networking = networking;
    }

}
