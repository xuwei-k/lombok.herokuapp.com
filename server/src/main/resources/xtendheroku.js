$(function(){
  $("#compile").click(
    function(){
      console.log("click!!");

      var sendData = {
        "files" : {
          "A.xtend": "class A{" + $("#xtendcode").val() + "}"
        }
      };

      console.log(sendData);

      jQuery.post(
        '/',
        JSON.stringify(sendData),
        function(data){
          console.log(data);
          if(! data.error){
            $("#javacode").text(data.result["A.java"])
          }else{
            $("#javacode").text(data.message)
          }
        },
        "JSON"
      );
    }
  );
});

$(document).ready(function(){
  $('#compile').click();
});