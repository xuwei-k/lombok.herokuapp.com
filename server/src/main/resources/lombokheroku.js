var changeEditType = function(t){
  console.log(t)
  if(t == "auto"){
    $(".lombok_class_wrap").show();
    $("#lombok_file_name_wrap").hide();
  }else if(t == "manual"){
    $(".lombok_class_wrap").hide();
    $("#lombok_file_name_wrap").show();
  }
}

$(function(){

  $("input[name='lombok_edit_type']").change(
    function(){
      var t = $("input[name='lombok_edit_type']:checked").val();
      changeEditType(t);
    }
  );

  $("#clear_javacode").click(
    function(){
      $("#javacode").children().remove();
    }
  );

  $("#clear_error_message").click(
    function(){
      $("#error_message").children().remove();
    }
  );

  $("#compile").click(
    function(){
      console.log("click!!");

      if($("input[name='lombok_edit_type']:checked").val() == "auto"){
        var class_name = $("#lombok_class_name").val();
        if(class_name == "" || class_name == null){
          return;
        }

        var file_name = class_name + ".java";
        var source_content = "class " + class_name + " {\n" + $("#lombokcode").val() + "}";
      }else{
        var class_name = $("#lombok_file_name").val()
        if(class_name == null || class_name == "")return;

        var file_name = class_name + ".java";
        var source_content = $("#lombokcode").val();
      }

      console.log(file_name);
      console.log(source_content);

      var sendData = {files:{}};

      sendData['files'][file_name] = source_content;

      console.log(sendData);

      $("#error_message").children().remove();
      $("#javacode").children().remove();

      jQuery.post(
        '/',
        JSON.stringify(sendData),
        function(data){
          console.log(data);
          if(! data.error){
            $("#javacode").text(data.result[class_name + ".java"]);
            prettyPrint();
          }else{
            $("#error_message").append("<pre>" + data.message + "</pre>")
          }
        },
        "JSON"
      );
    }
  );
});

$(document).ready(function(){
  $("#lombok_class_name").val("Hello")
  $("#edit_type_manual").attr('checked','checked');
  $("#lombok_file_name").val("Person");
  $("#lombokcode").val("import lombok.Data;\n\n@Data\npublic class Person{\n  private int age;\n  private final String name;\n}\n");
  changeEditType("manual");
  $('#compile').click();
});
