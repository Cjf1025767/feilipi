Ext.define('Rbac.view.main.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main',

    listen: {
        controller: {
            '#': {
                unmatchedroute: 'onRouteChange'
            }
        }
    },

    routes: {
        ':node': 'onRouteChange'
    },

    lastView: null,

    setCurrentView: function (hashTag) {
        hashTag = (hashTag || '').toLowerCase();

        var me = this,
            refs = me.getReferences(),
            mainCard = refs.mainCardPanel,
            mainLayout = mainCard.getLayout(),
            navigationList = refs.navigationTreeList,
            store = navigationList.getStore(),
            node = store.findNode('routeId', hashTag) ||
            store.findNode('viewType', hashTag),
            view = (node && node.get('viewType')) || 'dashboardView',
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
                routeId: hashTag, // for existingItem search later
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
            } else {
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
        menuBar.setText(node.getData().text);
        menuBar.setIconCls(node.getData().iconCls);

        if (newView.isFocusable(true)) {
            newView.focus();
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
        if (collapsing) {
            refs.senchaLogo.down('button').setIconCls('x-fa fa-forward');
        } else {
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
            wrapContainer.updateLayout(); // ... since this will flush them
        } else {
            if (!collapsing) {
                // If we are leaving micro mode (expanding), we do that first so that the
                // text of the items in the navlist will be revealed by the animation.
                navigationList.setMicro(false);
            }
            navigationList.canMeasure = false;

            // Start this layout first since it does not require a layout
            refs.senchaLogo.animate({
                dynamic: true,
                to: {
                    width: new_width
                }
            });

            // Directly adjust the width config and then run the main wrap container layout
            // as the root layout (it and its chidren). This will cause the adjusted size to
            // be flushed to the element and animate to that new size.
            navigationList.width = new_width;
            wrapContainer.updateLayout({
                isRoot: true
            });
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
    onUserNameBeforrender: function () {
        var controller = this;
        Ext.Ajax.request({
            url: '/tab/rbac/UIGetUserAuths',
            method : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            async: false,
            failure: function(response, opts) {                          
                console.log('server-side failure with status code ' + response.status);
            },
            success: function(response, opts) {
                var obj = Ext.decode(response.responseText);
                if(obj.success){ 
                    var sDisplay = obj.username;
                    if(obj.agent!=null && obj.agent.length>0){
                        sDisplay += "("+ obj.agent +")";
                    }
                    controller.getViewModel().setData({userName:sDisplay}); 
                }
            }
        });
    },
    onMainViewRender: function () {
        if (!window.location.hash) {
            this.redirectTo("dashboard");
        }
    },

    onRouteChange: function (id) {
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

    onNavigationTreeBeforeRender: function (me) {
        var controller = this,
            refs = me.getReferences(),
            navigationList = refs.navigationTreeList,
            resources = "45BBBF87-25B8-492A-B206-CC55F3E4903C" +
            ",6B46C170-833F-413C-9D90-D887817B3E34" +
            ",DCA604A2-BA58-449E-BF3C-3B9219C8B50C" +
            ",E09AEC8E-3169-4138-89BC-9AE6D7CFA955" +
            ",1A16A347-A2CF-11E9-9604-54E1AD6C1F93" +
            ",282CB471-EBC0-11E8-B69B-54E1AD6C1F93",
            removeResourceList = resources.split(",");
        Ext.Ajax.request({
            url: '/tab/rbac/UIGetOperations',
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            async: false,
            success: function (response, options) {
                var obj = Ext.decode(response.responseText);
                if (obj.success) {
                    obj.Operations.forEach(function (s) {
                        for (var i = 0; i < removeResourceList.length; ++i) {
                            if (s === removeResourceList[i]) {
                                removeResourceList.splice(i, 1); //删除已有权限
                                break;
                            }
                        }
                    });
                    var url = window.location.search; //获取url中"?"符后的字串
                    var queryParams = new Object();
                    if (url.indexOf("?") != -1) {
                        var str = url.substring(1);
                        strs = str.split("&");
                        for(var i = 0; i < strs.length; i ++) {
                            queryParams[strs[i].split("=")[0]] = decodeURI(strs[i].split("=")[1]);
                        }
                    }
                    if(window.location.hash==='#norecord' || queryParams['norecord']==="1"){
                        controller.removeNodes(navigationList.getStore(), '45BBBF87-25B8-492A-B206-CC55F3E4903C'); 
                       // controller.removeNodes(navigationList.getStore(), '282CB471-EBC0-11E8-B69B-54E1AD6C1F93');  
                    }
                    if(queryParams['nolist']==="1"){
                        refs.headerBar.setHidden(true);
                        navigationList.setHidden(true);
                        refs.senchaLogo.setWidth(0);
                        navigationList.setWidth(0);
                        navigationList.setMicro(true);
                    }
                    removeResourceList.forEach(function (s) {
                        controller.removeNodes(navigationList.getStore(), s);
                    });
                    controller.clearNodes(navigationList.getStore());
                }
            },
            failure: function (response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });
    },
    removeNodes: function (store, resourceId) {
        var node = store.findNode('resourceId', resourceId);
        while (node != null && node.parentNode != null) {
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