Ext.define('Tab.view.main.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main',

    listen : {
        controller : {
            '#' : {
                unmatchedroute : 'onRouteChange'
            }
        }
    },

    routes: {
        ':node': 'onRouteChange'
    },

    lastView: null,

    setCurrentView: function(hashTag) {
        hashTag = (hashTag || '').toLowerCase();
        if(hashTag.length==0){
            return;
        }
        var me = this,
            refs = me.getReferences(),
            mainCard = refs.mainCardPanel,
            mainLayout = mainCard.getLayout(),
            navigationList = refs.navigationTreeList,
            store = navigationList.getStore(),
            node = store.findNode('routeId', hashTag) ||
                   store.findNode('viewType', hashTag),
            view = (node && node.get('viewType')) || Tab.getApplication().getDefaultToken(),
            lastView = me.lastView,
            existingItem = mainCard.child('component[routeId=' + hashTag + ']'),
            newView;

        // Kill any previously routed window
        if (lastView && lastView.isWindow) {
            lastView.destroy();
        }

        lastView = mainLayout.getActiveItem();

        if (!existingItem) {
            newView = Ext.create({
                xtype: view,
                routeId: hashTag,  // for existingItem search later
                hideMode: 'offsets'
            });
        }

        if (!newView || !newView.isWindow) {
            // !newView means we have an existing view, but if the newView isWindow
            // we don't add it to the card layout.
            if (existingItem) {
                // We don't have a newView, so activate the existing view.
                if (existingItem !== lastView) {
                    mainLayout.setActiveItem(existingItem);
                }
                newView = existingItem;
            }
            else {
                // newView is set (did not exist already), so add it and make it the
                // activeItem.
                Ext.suspendLayouts();
                mainLayout.setActiveItem(mainCard.add(newView));
                Ext.resumeLayouts(true);
            }
        }

        navigationList.setSelection(node);
        //修改标题，同步菜单选择
        var menuBar = mainCard.up().up().down('#headerBar').down('#menuBar');
        if(node){
            menuBar.setText(node.getData().text);
            menuBar.setIconCls(node.getData().iconCls);
        }
        if (newView.isFocusable(true)) {
            newView.focus();
            me.getView().down('maincontainerwrap').setScrollY(0);
        }

        me.lastView = newView;
    },

    newCurrentView: function(xtype) {
        var me = this,
            refs = me.getReferences(),
            mainCard = refs.mainCardPanel,
            mainLayout = mainCard.getLayout(),
            navigationList = refs.navigationTreeList,
            lastView = me.lastView,
            existingItem = mainCard.child('component[routeId=' + xtype + ']'),
            newView;

        // Kill any previously routed window
        if (lastView && lastView.isWindow) {
            lastView.destroy();
        }

        lastView = mainLayout.getActiveItem();

        if (!existingItem) {
            newView = Ext.create({
                xtype: xtype,
                routeId: xtype,
                hideMode: 'offsets'
            });
        }

        if (!newView || !newView.isWindow) {
            if (existingItem) {
                if (existingItem !== lastView) {
                    mainLayout.setActiveItem(existingItem);
                }
                newView = existingItem;
            }
            else {
                Ext.suspendLayouts();
                mainLayout.setActiveItem(mainCard.add(newView));
                Ext.resumeLayouts(true);
            }
        }
        navigationList.setSelection(null);
        this.redirectTo("#");

        if (newView.isFocusable(true)) {
            newView.focus();
            me.getView().down('maincontainerwrap').setScrollY(0);
        }

        me.lastView = newView;
    },

    onNavigationTreeSelectionChange: function (tree, node) {
        var to = node && (node.get('routeId') || node.get('viewType'));

        if (to) {
            this.redirectTo(to);
        }
    },

    onToggleNavigationSize: function () {
        var me = this,
            refs = me.getReferences(),
            navigationList = refs.navigationTreeList,
            wrapContainer = refs.mainContainerWrap,
            collapsing = !navigationList.getMicro(),
            new_width = collapsing ? 48 : me.getView().tabNavigationWidth;
        if(collapsing){
            refs.senchaLogo.down('button').setIconCls('x-fa fa-forward');
        }else{
            refs.senchaLogo.down('button').setIconCls('x-fa fa-backward');
        }
        if (Ext.isIE9m || !Ext.os.is.Desktop) {
            Ext.suspendLayouts();

            refs.senchaLogo.setWidth(new_width);

            navigationList.setWidth(new_width);
            navigationList.setMicro(collapsing);

            Ext.resumeLayouts(); // do not flush the layout here...

            // No animation for IE9 or lower...
            wrapContainer.layout.animatePolicy = wrapContainer.layout.animate = null;
            wrapContainer.updateLayout();  // ... since this will flush them
        }
        else {
            if (!collapsing) {
                // If we are leaving micro mode (expanding), we do that first so that the
                // text of the items in the navlist will be revealed by the animation.
                navigationList.setMicro(false);
            }
            navigationList.canMeasure = false;

            // Start this layout first since it does not require a layout
            refs.senchaLogo.animate({dynamic: true, to: {width: new_width}});

            // Directly adjust the width config and then run the main wrap container layout
            // as the root layout (it and its chidren). This will cause the adjusted size to
            // be flushed to the element and animate to that new size.
            navigationList.width = new_width;
            wrapContainer.updateLayout({isRoot: true});
            navigationList.el.addCls('nav-tree-animating');

            // We need to switch to micro mode on the navlist *after* the animation (this
            // allows the "sweep" to leave the item text in place until it is no longer
            // visible.
            if (collapsing) {
                navigationList.on({
                    afterlayoutanimation: function () {
                        navigationList.setMicro(true);
                        navigationList.el.removeCls('nav-tree-animating');
                        navigationList.canMeasure = true;
                    },
                    single: true
                });
            }
        }
    },

    onMainViewRender:function() {
        if (!window.location.hash) {
            this.redirectTo(Tab.getApplication().getDefaultToken());
        }
    },

    onRouteChange:function(id){
        this.setCurrentView(id);
    },

    onSearchRouteChange: function () {
        this.setCurrentView('searchresults');
    },

    onSwitchToModern: function () {
        Ext.Msg.confirm('Switch to Modern', 'Are you sure you want to switch toolkits?',
                        this.onSwitchToModernConfirmed, this);
    },

    onSwitchToModernConfirmed: function (choice) {
        if (choice === 'yes') {
            var s = window.location.search;

            // Strip "?classic" or "&classic" with optionally more "&foo" tokens
            // following and ensure we don't start with "?".
            s = s.replace(/(^\?|&)classic($|&)/, '').replace(/^\?/, '');

            // Add "?modern&" before the remaining tokens and strip & if there are
            // none.
            window.location.search = ('?modern&' + s).replace(/&$/, '');
        }
    },
    onNavigationTreeBeforeRender:function(me){
        var controller = this,
        refs = me.getReferences(),
        navigationList = refs.navigationTreeList,
        resources = "9A611B6F-5664-4C43-9D06-C1E2141CCCB1" //超级管理
        +",328F332E-AEAE-11E9-80B6-000C294BD5A6"//呼出管理
        +",6E48F8F3-93EC-11E9-9604-54E1AD6C1F93" //呼出报表
        +",6C8605F6-B994-11E9-9EA9-54E1AD6C1F93"//坐席管理
        +",BBFED234-B994-11E9-9EA9-54E1AD6C1F93"//综合报表
        +",45BBBF87-25B8-492A-B206-CC55F3E4903C"//录音查询
        +",C454655B-B994-11E9-9EA9-54E1AD6C1F93"//坐席员
        +",B0ED5C1A-BCD7-11E9-BF43-54E1AD6C1F93"//满意度
        +",48E6621D-D3A2-11E9-80B6-000C294BD5A6"//短信模板
        +",952C8D60-4F8A-11EB-B5EB-00155D782604"//节假日设置
        +",282CB471-EBC0-11E8-B69B-54E1AD6C1F93"//录音分机设置
        +",1A16A347-A2CF-11E9-9604-54E1AD6C1F93"//座席监控
        +",3CA268C0-E098-4A8B-ABF7-86E215AE7F42"//录音标记
        +",4A16A347-A2CF-11E9-9604-54E1AD6C1F93"//分机监控
        +",6C8605F6-B994-11E9-9EA9-54E1AD6C1F94"//语音信箱
        ,
        removeResourceList = resources.split(",");
        Ext.Ajax.request({
            url : '/tab/rbac/UIGetOperations',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            async: false,
            success: function(response, options){
                var obj = Ext.decode(response.responseText);
                if(obj.success){
                    obj.Operations.forEach(function(s){
                        for(var i=0;i<removeResourceList.length;++i){
                            if(s===removeResourceList[i]){
                                removeResourceList.splice(i,1);//删除已有权限
                                break;
                            }
                        }
                        if(s=='9A611B6F-5664-4C43-9D06-C1E2141CCCB1'){//权限系统  //6B46C170-833F-413C-9D90-D887817B3E34
                            controller.getView().down('#usermanagerId').setHidden(false);
                        }else if(s=='C454655B-B994-11E9-9EA9-54E1AD6C1F93'){//坐席员
                            controller.getView().down('#softphoneId').setHidden(true);
                           // controller.getView().down('#softphoneId').setHidden(false);
                        }
                    });
                    var url = window.location.search; //获取url中"?"符后的字串
                    var queryParams = new Object();
                    if (url.indexOf("?") != -1) {
                        var str = url.substr(1);
                        strs = str.split("&");
                        for(var i = 0; i < strs.length; i ++) {
                            queryParams[strs[i].split("=")[0]] = decodeURI(strs[i].split("=")[1]);
                        }
                    }
                    if(queryParams['nolist']==="1"){
                        refs.headerBar.setHidden(true);
                        navigationList.setHidden(true);
                        refs.senchaLogo.setWidth(0);
                        navigationList.setWidth(0);
                        navigationList.setMicro(true);
                        globalVars.workspaceAgent=true
                    }else{
                        removeResourceList.forEach(function(s){
                            controller.removeNodes(navigationList.getStore(),s);
                        });
                        //隐藏misscalls 飞利浦的需求
                        controller.removeNodes(navigationList.getStore(),"BBFED234-B994-11E9-9EA9-54E1AD6C1F94");
                        controller.clearNodes(navigationList.getStore());
                    }
                }
            },failure: function(response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });	
    },
    removeNodes:function(store,resourceId){
        var node = store.findNode('resourceId', resourceId);
        while(node!=null && node.parentNode!=null){
            var parent = node.parentNode;
            parent.removeChild(node);
            node = store.findNode('resourceId', resourceId);
        }
    },
    clearNodes:function(store){
        //删除无子节点的目录
        store.each(function(node){
            if(node.get('leaf')==false && node.childNodes.length==0){
                var parent = node.parentNode;
                parent.removeChild(node);
            }
        });
    }
});
