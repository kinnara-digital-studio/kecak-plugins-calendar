package com.kinnarastudio.calendar;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.calendar.userview.CalendarMenu;
import com.kinnarastudio.calendar.userview.CalendarTimeLineMenu;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(CalendarMenu.class.getName(), new CalendarMenu(), null));
        registrationList.add(context.registerService(CalendarTimeLineMenu.class.getName(), new CalendarTimeLineMenu(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}