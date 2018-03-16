document.write('<h1>* Series Summaries</h1><table width="500" cellspacing="15"><tr><td>Edit the values below to customize the periods of series errors to summarize.</td><td>[<a href="javascript:helplink(\'http://inca.sdsc.edu/releases/latest/guide/userguide-consumer.html#SERIES-REPORT\')\;">help</a>]</td></tr></table>');

Ext.onReady(function() {
  createTreeSummary();
});

function saveTreeSummary(tree) {
  var xml = tree.toXmlString();
  Ext.Ajax.request( { 
    url: 'admin.jsp', 
    params: {xml: xml, file: 'weekSummary'}, 
    method: 'POST',
    success: function(){ Ext.Msg.alert("File Saved"); } 
  });
}

function createTreeSummary() {
  var tree = new Ext.tree.TreePanel({
    el:'weekSummary',
    width:500,
    autoHeight:true,
    autoScroll:true,
    expandable:true,
    enableDD:true,
    title: 'Series error summary periods',
    loader: new Ext.ux.XmlTreeLoader({ preloadChildren: true }),
    root: Ext.Ajax.request({
      url: '/inca/xml/weekSummary.xml',
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
      'textchange' : function(){ saveTreeSummary(tree); }
    },
    tbar: [{
      text:'+', listeners: { 'click' : function(){ tree.expandAll(); } }
    },{ xtype:'tbseparator' },{
      text:'-', listeners: { 'click' : function(){ tree.collapseAll(); tree.root.expand(); } }
    },{ xtype:'tbseparator' },{
      text:'Add Summary',
      listeners: {
        'click' : function(){
          handleCreate = function (btn, text, cBoxes){
            if(btn == 'ok' && text) {
              var sum = new Ext.tree.TreeNode({text:'summary', leaf:false, expandable:true });
              sum.appendChild( new Ext.tree.TreeNode({text:'title', leaf:false, expandable:true }) );
              sum.firstChild.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
              sum.appendChild( new Ext.tree.TreeNode({text:'beginIndex', leaf:false, expandable:true }) );
              sum.item(1).appendChild( new Ext.tree.TreeNode({text:'1', leaf:true, expandable:false }) );
              sum.appendChild( new Ext.tree.TreeNode({text:'endIndex', leaf:false, expandable:true }) );
              sum.item(2).appendChild( new Ext.tree.TreeNode({text:'1', leaf:true, expandable:false }) );
              var root = tree.getRootNode();
              root.insertBefore(sum, root.firstChild);
              saveTreeSummary(tree);
            }
          }
          Ext.MessageBox.show({ title:'Add Summary', msg: 'Name of Summary:', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Delete',
      listeners: {
        'click' : function(){
          var selectedItem = tree.getSelectionModel().getSelectedNode();
          if (!selectedItem) {
            Ext.Msg.alert('Warning', 'Please select an Item to delete.');
            return false;
          }
          handleDelete = function (btn){
            if(btn == 'ok') { 
              selectedItem.remove(); 
              saveTreeSummary(tree);
            }
          }
          var text = selectedItem.text;
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
                params: {defaults: 'weekSummary'}, 
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
