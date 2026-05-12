package com.clubchretien.abonnement.ws;

public class OperationInfo {
    private String commandReference;
    private String pin;
    private String devisTransaction;


    public OperationInfo(String commandReference, String pin, String devisTransaction) {
        this.commandReference = commandReference;
        this.pin = pin;
        this.devisTransaction = devisTransaction;
    }

    public OperationInfo() {
    }


    public String getCommandReference() {
        return commandReference;
    }

    public void setCommandReference(String commandReference) {
        this.commandReference = commandReference;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDevisTransaction() {
        return devisTransaction;
    }

    public void setDevisTransaction(String devisTransaction) {
        this.devisTransaction = devisTransaction;
    }
}
