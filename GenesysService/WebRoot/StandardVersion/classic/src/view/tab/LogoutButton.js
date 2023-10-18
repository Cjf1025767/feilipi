Ext.define('Tab.view.tab.LogoutButton',{
    extend: 'Ext.button.Button',
    xtype: 'logoutbutton',
    iconCls:'x-fa fa-sign-out',
    listeners:{
        click:function(){
            Ext.Ajax.request({
                url: '/tab/rbac/UILogout',
                method : 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                async: true,
                failure: function(response, opts) {                          
                    console.log('server-side failure with status code ' + response.status);
                },
                success: function(response, opts) {
                    window.location.href = "/login.html";
                }
            });
        }
    }
});