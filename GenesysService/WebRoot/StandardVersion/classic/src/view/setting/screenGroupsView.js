Ext.define('Tab.view.setting.screenGroupsView', {
    extend: 'Ext.grid.Panel',
    xtype: 'screenGroupsView',

    requires: [
        'Tab.view.setting.screenGroupsController',
        'Tab.view.setting.screenGroupsModel',
        'Tab.view.setting.ScreenGroupsStore',
    ],

    controller: 'setting-screengroups',
    viewModel: {
        type: 'setting-screengroups'
    },
    store: Ext.create('Tab.view.setting.ScreenGroupsStore'),
    
    layout: 'container',
    
    plugins: [{
        ptype: 'cellediting',
        clicksToEdit: 2,
    }],
    listeners: {
    	/*'beforerender':function(me){
            var weekboxs = me;
            Ext.Ajax.request({
                url: '/tab/crm/getShowGroups',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                params: {
                    nType: "0"
                },
                success: function (response, options) {
                    var obj = Ext.decode(response.responseText);
                    if (obj.success) {
                        weekboxs.store.load(obj.list);
                    }
                },
                failure: function (response, opts) {
                    console.log('server-side failure with status code ' + response.status);
                }
            });
        },*/
        'edit': function (editor, context, eOpts) {
            var ctx = context;
            Ext.Ajax.request({
                url: '/tab/crm/EditGroupNumber',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                params: {
                    id: ctx.record.get('id'),
                    groupNo: ctx.record.get('value'),
                    nType:ctx.record.store.proxy.extraParams.nType
                },
                success: function (response, options) {
                    var obj = Ext.decode(response.responseText);
                    if (obj.success) {
                        ctx.record.commit();
                    } else {
                        ctx.record.reject();
                    }
                },
                failure: function (response, opts) {
                    ctx.record.reject();
                    console.log('server-side failure with status code ' + response.status);
                }
            });
        }
    },
    tbar:{
    	items:[{
			xtype: 'button',
			width: 100,
			margin: '0 0 10 0',
			text: 'Query',
			listeners: {
				'click': function () {
					var me = this.up().up();
					me.store.proxy.extraParams = {
						nType: me.down('combobox[fieldLabel="Business type"]').getValue()
					};
					me.store.load();
				}
			}
		},
        {

			xtype: 'combobox',
			width: 240,
			fieldLabel: 'Business type',
			labelWidth: 60,
			queryMode: 'local',
			value: 0,
			store: Ext.create('Ext.data.JsonStore', {
				model: 'Tab.view.record.typeModel',
				data: [{
						"name": "Volkswagen large screen group number",
						"id": 0
					},
					{
						"name": "Audi large screen group number",
						"id": 1
					}
				]
			}),
			editable: false,
			triggerAction: 'all',
			displayField: 'name',
			valueField: 'id'
        }]
    },
    columns: [{
        xtype: 'rownumberer',
        width: 35,
        align: 'center'
      },
      {
    	  header: 'GroupName',
	      dataIndex: 'value',
	      flex:1,
	      editable: true,
	      menuDisabled: true,
	      align: 'center',
	      editor: {
              allowBlank: true
          }
      }
  ]
});