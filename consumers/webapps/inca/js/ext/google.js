document.write('<h1>* Google Maps</h1><table width="500" cellspacing="15"><tr><td>Edit the values below to configure Google maps for your installation.</td><td>[<a href="javascript:helplink(\'http://inca.sdsc.edu/releases/latest/guide/userguide-consumer.html#GOOGLE-SETUP\')\;">help</a>]</td></tr></table>');

Ext.onReady(function() {
  createTree();
});

function saveTree(tree) {
  var xml = tree.toXmlString();
  Ext.Ajax.request( { 
    url: 'admin.jsp', 
    params: {xml: xml, file: 'google'}, 
    method: 'POST', 
    success: function(){ Ext.Msg.alert("File Saved") } 
  }); 
}

function newTest(text) {
  var test = new Ext.tree.TreeNode({text:'test', leaf:false, expandable:true });
  var name = test.appendChild( new Ext.tree.TreeNode({text:'name', leaf:false, expandable:true }) );
  name.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
  var regex = test.appendChild( new Ext.tree.TreeNode({text:'regex', leaf:false, expandable:true }) );
  regex.appendChild( new Ext.tree.TreeNode({text:'Insert regex here', leaf:true, expandable:false }) );
  return test;
}

function newSite(text) {
  var site = new Ext.tree.TreeNode({text:'site', leaf:false, expandable:true });
  site.appendChild( new Ext.tree.TreeNode({text:'name', leaf:false, expandable:true }) );
  site.firstChild.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
  site.appendChild( new Ext.tree.TreeNode({text:'latitude', leaf:false, expandable:true }) );
  site.item(1).appendChild( new Ext.tree.TreeNode({text:'37', leaf:true, expandable:false }) );
  site.appendChild( new Ext.tree.TreeNode({text:'longitude', leaf:false, expandable:true }) );
  site.item(2).appendChild( new Ext.tree.TreeNode({text:'982', leaf:true, expandable:false }) );
  site.appendChild( new Ext.tree.TreeNode({text:'logo', leaf:false, expandable:true }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'url', leaf:false, expandable:true }) );
  site.item(3).firstChild.appendChild( new Ext.tree.TreeNode({text:'http://sapa.sdsc.edu:8080/inca/img/inca-powered-by.jpg', leaf:true, expandable:false }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'width', leaf:false, expandable:true }) );
  site.item(3).item(1).appendChild( new Ext.tree.TreeNode({text:'125', leaf:true, expandable:false }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'height', leaf:false, expandable:true }) );
  site.item(3).item(2).appendChild( new Ext.tree.TreeNode({text:'47', leaf:true, expandable:false }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'angle', leaf:false, expandable:true }) );
  site.item(3).item(3).appendChild( new Ext.tree.TreeNode({text:'180', leaf:true, expandable:false }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'logoAnchorX', leaf:false, expandable:true }) );
  site.item(3).item(4).appendChild( new Ext.tree.TreeNode({text:'125', leaf:true, expandable:false }) );
  site.item(3).appendChild( new Ext.tree.TreeNode({text:'logoAnchorY', leaf:false, expandable:true }) );
  site.item(3).item(5).appendChild( new Ext.tree.TreeNode({text:'33', leaf:true, expandable:false }) );
  site.appendChild( new Ext.tree.TreeNode({text:'resources', leaf:false, expandable:true }) );
  site.item(4).appendChild( newResource(text+'-resource') );
  return site;
}

function newResource(text) {
  var resource = new Ext.tree.TreeNode({text:'resource', leaf:false, expandable:true });
  resource.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
  return resource;
}

function createTree() {
  var tree = new Ext.tree.TreePanel({
    el:'google',
    width:500,
    autoHeight:true,
    autoScroll:true,
    expandable:true,
    enableDD:true,
    title: 'Google status page maps',
    loader: new Ext.ux.XmlTreeLoader({ preloadChildren: true }),
    root: Ext.Ajax.request({ 
      url: '/inca/xml/google.xml', 
      success: function(response){
        var root = response.responseXML.documentElement || response.responseXML;
        var node = new Ext.tree.AsyncTreeNode({ text: root.tagName, xmlNode: root });
        tree.setRootNode(node);
        tree.expandAll();
        tree.collapseAll();
        tree.root.expand();
      } 
    }),
    listeners: {
      'textchange' : function(){ saveTree(tree); }
    },
    tbar: [{
      text:'+', listeners: { 'click' : function(){ tree.expandAll(); } }
    },{ xtype:'tbseparator' },{
      text:'-', listeners: { 'click' : function(){ tree.collapseAll(); tree.root.expand(); } }
    },{ xtype:'tbseparator' },{
      text:'Add Cross Site Test',
      listeners: {
        'click' : function(){
          handleCreate = function (btn, text, cBoxes){
            if(btn == 'ok' && text) {
              var root = tree.getRootNode();
              var crossSite = root.findChild( "text", "crossSite" );
              if ( crossSite == null ) {
                // insert crossSite tag before sites tag
                var sites = root.findChild( "text", "sites" );
                crossSite = new Ext.tree.TreeNode({text:'crossSite', leaf:false, expandable:true });
                root.insertBefore( crossSite, sites )
              }
              crossSite.appendChild(newTest(text));
              saveTree(tree);
            }
          }
          Ext.MessageBox.show({ title:'Add Cross Site Test', msg: 'Name of test:', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Add Site',
      listeners: {
        'click' : function(){
          handleCreate = function (btn, text, cBoxes){
            if(btn == 'ok' && text) {
              var site = newSite(text);
              tree.getRootNode().item(10).appendChild(site);
              saveTree(tree);
            }
          }
          Ext.MessageBox.show({ title:'Add Site', msg: 'Name of Site:', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Add Resource',
      listeners: {
        'click' : function(){
          var selected = tree.getSelectionModel().getSelectedNode();
          if (!selected || (selected.parentNode.text != "site" || selected.text != "resources")){
            Ext.Msg.alert("You must first select a site 'resources' folder to add a resource.");
          } else {
            handleCreate = function (btn, text, cBoxes){
              if(btn == 'ok' && text) {
                selected.insertBefore(newResource(text), selected.item(1)); 
                saveTree(tree);
              }
            }
            Ext.MessageBox.show({ title:'Add Resource', msg: 'Name of Resource', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
          }
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Delete',
      listeners: {
        'click' : function(){
          var selected = tree.getSelectionModel().getSelectedNode();
          if (!selected || (selected.parentNode.text!="sites" && selected.parentNode.text!="resources" && 
                            selected.parentNode.text!="crossSite") || (selected.text!="test" && 
                            selected.text!="site" && selected.text!="resource") ){
            Ext.Msg.alert("Warning", "Please select an 'site', 'resource' or 'test' folder to delete.");
            return false;
          }
          handleDelete = function (btn){
            if(btn == 'ok') { 
              selected.remove(); 
              saveTree(tree);
            }
          }
          var text = selected.text;
          Ext.MessageBox.show({
            title:'Confirm your action',
            msg: "Are you sure you want to delete '"+text+"' and everything under it?",
            buttons: Ext.MessageBox.OKCANCEL,
            fn: handleDelete
          });
        }
      }
    },{ xtype:'tbseparator' },{
      text: 'Restore to Defaults',
      listeners: {
        'click' : function(){
          handleDefault = function (btn){
            if(btn == 'ok') {  
              Ext.Ajax.request({ 
                url: 'admin.jsp', 
                params: {defaults: 'google'}, 
                method: 'POST', 
                success: function(){ window.location.reload(true); Ext.Msg.alert("Defaults Restored");} 
              });
            }
          }
          Ext.MessageBox.show({
            title:'Confirm your action',
            msg: 'Are you sure you want to restore defaults?',
            buttons: Ext.MessageBox.OKCANCEL,
            fn: handleDefault
          });
        }
      }
    }]
  });
  tree.render();
  var te = new Ext.tree.TreeEditor(tree, null, {
    editDelay: 0,
    beforeNodeClick : Ext.emptyFn,
    onNodeDblClick : function(node, e){
      e.stopEvent();
      this.triggerEdit(node);
    }
  });
}
