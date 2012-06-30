var changeEditType = function(t){
  console.log(t)
  if(t == "auto"){
    $(".xtend_class_wrap").show();
    $("#xtend_file_name_wrap").hide();
  }else if(t == "manual"){
    $(".xtend_class_wrap").hide();
    $("#xtend_file_name_wrap").show();
  }
}

$(function(){

  $("input[name='xtend_edit_type']").change(
    function(){
      var t = $("input[name='xtend_edit_type']:checked").val();
      changeEditType(t);
    }
  );

  $("#compile").click(
    function(){
      console.log("click!!");

      if($("input[name='xtend_edit_type']:checked").val() == "auto"){
        var class_name = $("#xtend_class_name").val();
        if(class_name == "" || class_name == null){
          return;
        }

        var file_name = class_name + ".xtend";
        var source_content = "class " + class_name + " {\n" + $("#xtendcode").val() + "}";
      }else{
        var class_name = $("#xtend_file_name").val()
        if(class_name == null || class_name == "")return;

        var file_name = class_name + ".xtend";
        var source_content = $("#xtendcode").val();
      }

      console.log(file_name);
      console.log(source_content);

      var sendData = {files:{}};

      sendData['files'][file_name] = source_content;

      console.log(sendData);

      jQuery.post(
        '/',
        JSON.stringify(sendData),
        function(data){
          console.log(data);
          if(! data.error){
            $("#javacode").text(data.result[class_name + ".java"]);
            prettyPrint();
          }else{
            $("#error_message").append(data.html_message)
          }
        },
        "JSON"
      );
    }
  );
});

$(document).ready(function(){
  $("#xtend_class_name").val("Hello")
  $("#edit_type_manual").attr('checked','checked');
  $("#xtend_file_name").val("Person");
  $("#xtendcode").val("@Data class Person{\n  String name\n  int age\n}\n");
  changeEditType("manual");
  $('#compile').click();
});
