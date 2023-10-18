Ext.define('Tab.view.main.batchViewController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main-batchview',

    onAfterrenderBatchList:function(grid,eOpts){
        grid.getStore().load();
    },
    onBatchClick:function(grid, td, cellIndex, record, tr, rowIndex, e, eOpts){
        var phonegrid = grid.up().up().down('gridpanel[name=phone]');
        if(phonegrid.store.proxy.extraParams.id!=record.get('id')){
            Ext.apply(phonegrid.store.proxy.extraParams,{id:record.get('id')});
            phonegrid.store.reload();
        }
        var col = grid.getHeaderCt().getHeaderAtIndex(cellIndex).dataIndex;
        var rs = record;
        if(col=='activate'){
            Ext.Ajax.request({
                url : '/tab/call/AddOrUpdateBatch',
                method : 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                params: {
                    id:record.get('id'),
                    activate:record.get('activate')
                },
                success: function(response, options){
                    var obj = Ext.decode(response.responseText);
                    if(obj.success){
                        rs.commit();
                    }else{
                        rs.reject();
                    }
                },failure: function(response, opts) {
                    rs.reject();
                    console.log('server-side failure with status code ' + response.status);
                }
            });
        }
    },
    onBatchPhone:function(btn){
        var phone = btn.up().down('textfield').value,
        phonegrid = btn.up('gridpanel');
        Ext.apply(phonegrid.store.proxy.extraParams,{phone:phone});
        phonegrid.store.reload();
    },
    onPreviewBatch:function(btn){
        var grid = btn.up('window').down('gridpanel'),
        form = btn.up('window').down('form').getForm();
        // form.standardSubmit=true;
        if (form.isValid()) {
            form.submit({
                url: '/tab/call/PreviewBatch',
                success: function(fp, value) {
                    grid.getStore().loadData(value.result.data,true);
                    form.setValues({fid:value.result.fid});
                },
                failure: function(form, action) {                          
                    switch (action.failureType) {
                        case Ext.form.action.Action.CLIENT_INVALID:
                            Ext.Msg.alert('失败', '您输入的某些内容无效');
                            break;
                        case Ext.form.action.Action.CONNECT_FAILURE:
                            Ext.Msg.alert('失败', '网络通讯错误');
                            break;
                        case Ext.form.action.Action.SERVER_INVALID:
                            Ext.Msg.alert('失败', action.result.msg);
                            break;
                    }
                }
            });
        }
    },
    onExcelClick:function(btn, td, cellIndex, record, tr, rowIndex, e, eOpts){
        var form = btn.up('window').down('form').getForm();
        form.setValues({row:rowIndex,col:cellIndex});
    },
    onAddBatchConfirm:function(btn){
        var win = btn.up('window'),
        form = btn.up('window').down('form').getForm(),
        values = form.getValues();
        Ext.Ajax.request({
            url : '/tab/call/AddOrUpdateBatch',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            params: {
                activate:true,
                description:values.description,
                name:values.name,
                fid:values.fid,
                col:values.col,
                row:values.row
            },
            success: function(response, options){
                var obj = Ext.decode(response.responseText);
                if(obj.success){
                    win.gridstore.insert(0, obj.item);
                    win.close();
                }else{
                    Ext.Msg.alert("错误",obj.msg);
                }
            },failure: function(response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });
    },
    onBatchEdit:function(editor, context, eOpts){
        var ctx = context;
        Ext.Ajax.request({
            url : '/tab/call/AddOrUpdateBatch',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            params: {
                id:ctx.record.get('id'),
                name:ctx.record.get('name'),
                description:ctx.record.get('description')
            },
            success: function(response, options){
                var obj = Ext.decode(response.responseText);
                if(obj.success){
                    ctx.record.commit();
                }else{
                    ctx.record.reject();
                }
            },failure: function(response, opts) {
                ctx.record.reject();
                console.log('server-side failure with status code ' + response.status);
            }
        });
    }
});
