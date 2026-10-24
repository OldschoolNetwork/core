package com.oldschoolnetwork.core;

import com.oldschoolnetwork.core.IONCoreService;

public interface ServiceAwareFragment {
    void setCoreService(IONCoreService service);
    void onSearch(String query);
}
