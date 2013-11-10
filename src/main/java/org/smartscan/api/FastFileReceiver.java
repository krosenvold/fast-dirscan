package org.smartscan.api;

import org.smartscan.api.FastFile;

public interface FastFileReceiver
{
    public void accept(FastFile file);
}
