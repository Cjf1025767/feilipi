Ext.define('Tab.view.setting.extensionSettingViewModel', {
  extend: 'Ext.app.ViewModel',
  alias: 'viewmodel.setting-extensionsettingview',
  data: {
    name: 'WebServer'
  }
})
Ext.define('Tab.view.setting.ExtensionSettingModel', {
  extend: 'Ext.data.Model',
  fields: [
    {
      name: 'extension',
      mapping: 'extension'
    },
    {
      name: 'department',
      mapping: 'department'
    }
  ]
})
Ext.define('Tab.view.setting.ExtensionSettingStore', {
  extend: 'Ext.data.Store',
  model: 'Tab.view.setting.ExtensionSettingModel',
  autoload: true,
  fields: [
    {
      name: 'extension',
      mapping: 'extension'
    }
  ],
  proxy: {
    type: 'rest',
    url: '/tab/call/GetExtensionList',
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
