Ext.define('Tab.model.Base', {
    extend: 'Ext.data.Model',

    schema: {
        namespace: 'Tab.model'
    }
});

Ext.define('Tab.model.groupsMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'roleguid',
    fields: [
        { type: 'string', name: 'roleguid' },
        { type: 'string', name: 'rolename' }
    ]
});

Ext.define('Tab.model.groupsMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.groupsMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetRolesManageable',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
    listeners:{
    	load:{
    		scope:this,
    		fn:function(store, records){ 
                store.insert(0,Ext.create('Tab.model.groupsMenuModel',{roleguid:'00000000-0000-0000-0000-000000000000',rolename:'All'}));
    		}
    	}
    }
});

Ext.define('Tab.model.groupsStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.groupsMenuModel',
    autoLoad: false,
    remoteSort: true,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetRolesForRole',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
 
});

Ext.define('Tab.model.queuesStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.groupsMenuModel',
    autoLoad: false,
    remoteSort: true,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetAllQueues',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
    listeners:{
    	load:{
    		scope:this,
    		fn:function(store, records){ 
                store.insert(0,Ext.create('Tab.model.groupsMenuModel',{userguid:'00000000-0000-0000-0000-000000000001',rolename:'NONE'}));
    		}
    	}
    }
});
Ext.define('Tab.model.usersMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'userguid',
    fields: [
        { type: 'string', name: 'userguid' },
        { type: 'string', name: 'nickname' }
    ]
});
Ext.define('Tab.model.digitModel', {
    extend: 'Ext.data.Model',
    fields: [
        { type: 'string', name: 'name' },
        { type: 'string', name: 'value' }
    ]
});
Ext.define('Tab.model.firstdigitStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.digitModel',
    autoLoad: false,
    remoteSort: true,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/GetFirstDigit',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
});
Ext.define('Tab.model.seconddigitStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.digitModel',
    autoLoad: false,
    remoteSort: true,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/GetSecondDigit',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
});

Ext.define('Tab.model.usersMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.model.usersMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetUsersForRoleManageableTK',
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
            type: 'json',
            rootProperty: 'Users',
            totalProperty: 'totalCount'
        }
    },
    listeners:{
    	load:{
    		scope:this,
    		fn:function(store, records){ 
                store.insert(0,Ext.create('Tab.model.usersMenuModel',{userguid:'00000000-0000-0000-0000-000000000000',nickname:'All'}));
    		}
    	}
    }
});

