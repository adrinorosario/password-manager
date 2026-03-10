package com.adrino.passmanager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IRemoteVaultSync extends Remote {

    String syncVault(String username,
            List<VaultEntryBean> entries,
            ISyncCallback callback) throws RemoteException;

    String sendMessage(String sender, String message) throws RemoteException;

    String ping() throws RemoteException;
}