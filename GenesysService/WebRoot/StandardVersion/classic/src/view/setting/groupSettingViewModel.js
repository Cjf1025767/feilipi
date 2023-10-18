Ext.define('Tab.view.setting.groupSettingViewModel', {
  extend: 'Ext.app.ViewModel',
  alias: 'viewmodel.setting-groupsettingview',
  data: {
    name: 'WebServer'
  }
})

Ext.define('Tab.view.setting.GroupSettingModel', {
  extend: 'Ext.data.Model',
  fields: [
    {
      name: 'skillGroup',
      mapping: 'skillGroup'
    },
    {
      name: 'number',
      mapping: 'number'
    },
    {
      name: 'value',
      mapping: 'value'
    },
    {
      name: 'department',
      mapping: 'department'
    }
  ]
})
Ext.define('Tab.view.setting.GroupSettingStore', {
  extend: 'Ext.data.Store',
  model: 'Tab.view.setting.GroupSettingModel',
  autoLoad: false,
  proxy: {
    type: 'rest',
    url: '/tab/call/GetGroupList',
    timeout: 120000,
    actionMethods: { read: 'POST' },
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    reader: {
      type: 'json',
      rootProperty: 'list',
      totalProperty: 'totalCount'
    }
  }
})
