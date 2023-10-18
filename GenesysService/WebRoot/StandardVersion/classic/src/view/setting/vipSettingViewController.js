Ext.define('Tab.view.setting.vipSettingViewController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.setting-vipsettingview',
    
    onCustomerInfoList:function(grid,eOpts){
        grid.getStore().load();
    },
    previewCustomerInfo:function(btn){
    	var grid = btn.up('window').down('gridpanel'),
        	form = btn.up('window').down('form').getForm();
    	if (form.isValid()) {
        	form.submit({
                url: '/tab/call/PreviewBatch',
                waitMsg: '正在上传,请稍后……',
                success: function(fp, value) {
                	grid.getStore().loadData(value.result.data,true);
                    form.setValues({fid:value.result.fid});
                },
                failure: function(form, action) {
                    Ext.Msg.alert("失败", Ext.JSON.decode(this.response.responseText).message);
                }
            });
        }
    },
    uploadVipFileSubmit:function(btn){
    	var win = btn.up('window'),
        form = btn.up('window').down('form').getForm(),
        values = form.getValues();
        Ext.Ajax.request({
            url : '/tab/call/UploadBusinessType',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            params: {
                fid:values.fid,
                col:values.col,
                row:values.row,
                customerType:1
            },
            success: function(response, options){
                var obj = Ext.decode(response.responseText);
                if(obj.success){
                    win.gridstore.insert(0, obj.item);
                    win.close();
                }else{
                    Ext.Msg.alert("错误",obj.msg);
                }
            },
            failure: function(response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });
    },
    uploadBlockFileSubmit:function(btn){
    	var win = btn.up('window'),
        form = btn.up('window').down('form').getForm(),
        values = form.getValues();
        Ext.Ajax.request({
            url : '/tab/call/UploadBusinessType',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            params: {
                fid:values.fid,
                col:values.col,
                row:values.row,
                customerType:0
            },
            success: function(response, options){
                var obj = Ext.decode(response.responseText);
                if(obj.success){
                    win.gridstore.insert(0, obj.item);
                    win.close();
                }else{
                    Ext.Msg.alert("错误",obj.msg);
                }
            },
            failure: function(response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });
    },
    ready: false,
    beforeRender: function () {
        this.ready = true;
	},
});