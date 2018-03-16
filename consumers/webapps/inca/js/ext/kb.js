document.write('<h1>* Knowledge Base</h1><table width="500" cellspacing="15"><tr><td>Edit the values below to configure the knowledge base for your installation.</td><td>[<a href="javascript:helplink(\'http://inca.sdsc.edu/releases/latest/guide/userguide-consumer.html#CONSUMER-KB\')\;">help</a>]</td></tr></table>');

Ext.onReady(function() {
  createTreeKb();
});

function saveTreeKb(tree) {
  var xml = tree.toXmlString();
  xml = xml.replace(/&amp;/g, "&");
  xml = xml.replace(/&/g, "&amp;");
  Ext.Ajax.request( { 
    url: 'admin.jsp', 
    params: {xml: xml, file: 'kb'}, 
    method: 'POST',
    success: function(){ Ext.Msg.alert("File Saved"); } 
  });
}

function createTreeKb() {
  var tree = new Ext.tree.TreePanel({
    el:'kb',
    width:500,
    autoHeight:true,
    autoScroll:true,
    expandable:true,
    enableDD:true,
    title: 'Knowledge base configuration',
    loader: new Ext.ux.XmlTreeLoader({ preloadChildren: true }),
    root: Ext.Ajax.request({
      url: '/inca/xml/kb.xml',
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
      'textchange' : function(){ saveTreeKb(tree); }
    },
    tbar: [{
      text:'+', listeners: { 'click' : function(){ tree.expandAll(); } }
    },{ xtype:'tbseparator' },{
      text:'-', listeners: { 'click' : function(){ tree.collapseAll(); tree.root.expand(); } }
    },{ xtype:'tbseparator' },{
      text: 'Restore to Defaults',
      listeners: {
        'click' : function(){
          handleDefault = function (btn){
            if(btn == 'ok') {  
              Ext.Ajax.request({ 
                url: 'admin.jsp', 
                params: {defaults: 'kb'}, 
                method: 'POST', 
                success: function(){ window.location.reload(true); Ext.Msg.alert("Defaults Restored"); } 
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
