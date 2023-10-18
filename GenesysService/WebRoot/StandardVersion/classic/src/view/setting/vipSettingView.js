Ext.define('Tab.view.setting.vipSettingView', {
    extend: 'Ext.panel.Panel',
    xtype: 'vipSettingView',

    requires: [
        'Tab.view.setting.vipSettingViewController',
        'Tab.view.setting.vipSettingViewModel',
        'Tab.view.setting.VipSettingStore',
        'Ext.form.*',
        'Ext.view.View',
        'Ext.ux.BoxReorderer',
        'Ext.ux.DataView.Animated'
    ],

    controller: 'setting-vipsettingview',
    viewModel: {
        type: 'setting-vipsettingview'
    },

    defaults:{
    	 anchor : '90%',
    	 layout: 'hbox'
    },
 
    store: Ext.create('Tab.view.setting.VipSettingStore'),
    items: [
    	{
    		 xtype: 'container',
    		 layout: {
	            type: 'hbox',
	            align: 'stretch'
	         },
	         defaults:{
	        	 anchor : '90%'
	         },
	         items:[{
	        	 flex:1,
	        	 store: Ext.create('Tab.view.setting.VipSettingStore'),
	        	 tbar: {
	        	        plugins: {
	        	            boxreorderer: {
	        	                listeners: {
	        	                    drop: 'updateStoreSorters'
	        	                }
	        	            }
	        	        },

	        	        items: [{
		                     text: 'Import Information',
		                     width: 100,
		                     handler: function (btn) {
		                         Ext.create("Ext.window.Window", {
		                             title: "Customer information",
		                             width: 400,
		                             height: 360,
		                             modal: true,
		                             resizable: false,
		                             bodyPadding: '5 0 0 5',
		                             gridstore: btn.up().store,
		                             controller: 'setting-vipsettingview',
		                             items: [
		                                 {
		                                     xtype: "form",
		                                     defaults: {
		                                         anchor: '100%'
		                                     },
		                                     fieldDefaults: {
		                                         labelAlign: "left",
		                                         flex: 1,
		                                         margin: 5
		                                     },
		                                     items: [
		                                         {
		                                             xtype: "fieldcontainer",
		                                             layout: "vbox",
		                                             items: [
		                                            	 { xtype: 'hiddenfield', name: 'fid' },
		                                                 {
		                                                     xtype: 'filefield',
		                                                     fieldLabel: 'Excel',
		                                                     labelWidth: 40,
		                                                     width: '100%',
		                                                     name: 'filename',
		                                                     buttonText: '',

		                                                     buttonConfig: {
		                                                         iconCls: 'fa-file-excel-o'
		                                                     },
		                                                     validator: function (value) {
		                                                         var arrType = value.split('.');
		                                                         var docType = arrType[arrType.length - 1].toLowerCase();
		                                                         if (docType == 'xlsx') {
		                                                             return true;
		                                                         } if (docType == 'xls') {
		                                                             return true;
		                                                         }
		                                                         return 'File type must be excel';
		                                                     }
		                                                 },
		                                                 {
		                                                     xtype: "fieldcontainer",
		                                                     layout: "hbox",
		                                                     items: [
		                                                         { xtype: "textfield", width: 190, name: "row", fieldLabel: "Select start line in Preview", labelWidth: 125, allowBlank: false, value: '0' },
		                                                         { xtype: "textfield", width: 130, name: "col", fieldLabel: "Import columns", labelWidth: 60, allowBlank: false, value: '0' }
		                                                     ]
		                                                 },
		                                                 {
		                                                     xtype: 'gridpanel',
		                                                     name: 'phone',
		                                                     width: '100%',
		                                                     border: 1,
		                                                     height: 150,
		                                                     scrollable: true,
		                                                     store: Ext.create('Ext.data.Store', {
		                                                         fields: ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"]
		                                                     }),
		                                                     selModel: 'cellmodel',
		                                                     listeners: {
		                                                         cellclick: 'onExcelClick'
		                                                     },
		                                                     columns: [
		                                                         { text: 'A', dataIndex: 'A', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'B', dataIndex: 'B', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'C', dataIndex: 'C', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'D', dataIndex: 'D', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'E', dataIndex: 'E', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'F', dataIndex: 'F', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'G', dataIndex: 'G', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'H', dataIndex: 'H', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'I', dataIndex: 'I', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'J', dataIndex: 'J', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'K', dataIndex: 'K', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'L', dataIndex: 'L', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'M', dataIndex: 'M', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'N', dataIndex: 'N', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'O', dataIndex: 'O', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'P', dataIndex: 'P', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'Q', dataIndex: 'Q', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'R', dataIndex: 'R', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'S', dataIndex: 'S', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'T', dataIndex: 'T', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'U', dataIndex: 'U', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'V', dataIndex: 'V', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'W', dataIndex: 'W', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'X', dataIndex: 'X', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'Y', dataIndex: 'Y', width: 60, menuDisabled: true, sortable: false },
		                                                         { text: 'Z', dataIndex: 'Z', width: 60, menuDisabled: true, sortable: false }
		                                                     ]
		                                                 }
		                                             ]
		                                         }
		                                     ]
		                                 }],
		                             buttonAlign: 'center',
		                             buttons: [
		                                 {
		                                     xtype: "button",
		                                     text: "VIPCustomer",
		                                     handler: 'uploadVipFileSubmit'
		                                 },{
											xtype: "button",
											text: "BlockCustomer",
											handler: 'uploadBlockFileSubmit'
										}, 
										 {
		                                     xtype: "button",
		                                     text: "Preview",
		                                     handler: 'previewCustomerInfo'
		                                 }
		                             ]
		                         }).show();
		                     }
		                 },
		                 {
		                     xtype: 'button',
		                     text: 'Refresh',
		                     width: 100,
		                     handler: function () {
		                         var view = this.up().up();
		                         view.store.load();
		                     }
		                 }]
	        	    },
	        	    items:{
	        	    	xtype: 'dataview',
	                    reference: 'dataview',
	                    plugins: {
	                        'ux-animated-dataview': true
	                    },
	                    margin:'20 10',
	                    itemSelector: 'pure-table pure-table-bordered',
	                    store: Ext.create('Tab.view.setting.VipSettingStore'),
	                    tpl: [
	                    	'<table class="pure-table pure-table-bordered">',
	                    		'<thead><tr><td colspan="5">PhoneNumber</td></tr></thead>',
			                    '<tbody>{[this.dataSlice(values,5)]}</tbody>',
	                        '</table>',
	                        {
	                    		compiled:true,
	                    		dataSlice:function(array,size){
	                    			var result = [],obj="",j=0;
	                    		    for (var x = 0; x < Math.ceil(array.length / size); x++) {
	                    		        var start = x * size;
	                    		        var end = start + size;
	                    		        result.push(array.slice(start, end));
	                    		    }
	                    		    while(j<result.length){
	                    		    	if(result[j].length == size){
	                    		    		obj += '<tr><td>'+result[j][0].phone+'</td>'
			                    		    + '<td>'+result[j][1].phone+'</td>'
			                    		    + '<td>'+result[j][2].phone+'</td>'
			                    		    + '<td>'+result[j][3].phone+'</td>'
			                    		    + '<td>'+result[j][4].phone+'</td><tr>';
	                    		    	}else if(result[j].length == 4){
	                    		    		obj += '<tr><td>'+result[j][0].phone+'</td>'
			                    		    + '<td>'+result[j][1].phone+'</td>'
			                    		    + '<td>'+result[j][2].phone+'</td>'
			                    		    + '<td>'+result[j][3].phone+'</td>'
			                    		    + '<td></td><tr>';
	                    		    	}else if(result[j].length == 3){
	                    		    		obj += '<tr><td>'+result[j][0].phone+'</td>'
			                    		    + '<td>'+result[j][1].phone+'</td>'
			                    		    + '<td>'+result[j][2].phone+'</td>'
			                    		    + '<td></td><td></td><tr>';
	                    		    	}else if(result[j].length == 2){
	                    		    		obj += '<tr><td>'+result[j][0].phone+'</td>'
			                    		    + '<td>'+result[j][1].phone+'</td>'
			                    		    + '<td></td><td></td><td></td><tr>';
	                    		    	}else{
	                    		    		obj += '<tr><td>'+result[j][0].phone+'</td>'
			                    		    + '<td></td><td></td><td></td><td></td><tr>';
	                    		    	}
	                    		    	j++;
	                    		    }
	                    		    return  obj;
	                    		}
	                        }
	                    ]
	        	    }
	         }]
    	}
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});