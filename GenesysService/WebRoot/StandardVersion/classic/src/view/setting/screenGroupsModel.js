Ext.define('Tab.view.setting.screenGroupsModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.setting-screengroups',
    data: {
        name: 'WebServer'
    }
});

Ext.define('Tab.view.setting.ScreenGroupsModel', {
	extend:'Ext.data.Model',
	fields:[
		{
	      name: 'vs'
	    }
	]
});
Ext.define('Tab.view.setting.ScreenGroupsStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.setting.ScreenGroupsModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/crm/QueryShowGroups',
        timeout: 120000,
        actionMethods: { read: 'POST' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader: {
          type: 'json',
          rootProperty: 'list',
          totalProperty: 'totalCount'
        }
      }
});
