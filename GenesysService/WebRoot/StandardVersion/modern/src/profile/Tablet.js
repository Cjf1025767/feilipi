Ext.define('Tab.profile.Tablet', {
    extend: 'Ext.app.Profile',

    requires: [
        'Tab.view.tablet.*'
    ],

    // Map tablet/desktop profile views to generic xtype aliases:
    //
    views: {
        email: 'Tab.view.tablet.email.Email',
        inbox: 'Tab.view.tablet.email.Inbox',
        compose: 'Tab.view.tablet.email.Compose',

        searchusers: 'Tab.view.tablet.search.Users'
    },

    isActive: function () {
        return !Ext.platformTags.phone;
    }
});
