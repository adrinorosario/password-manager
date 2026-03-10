package com.adrino.passmanager;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISyncCallback extends Remote {

    void onProgress(String message, int percent) throws RemoteException;

    void onSyncComplete(String summary) throws RemoteException;

    void onError(String errorMessage) throws RemoteException;
}