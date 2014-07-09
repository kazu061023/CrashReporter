<?php
mb_language("Ja");
mb_internal_encoding("SJIS");

$mailto=$_POST["toMailAddress"];
$subject=$_POST["crashDataTitle"];
$body=$_POST["crashDataBody"];
$mailfrom="From:" .$_POST["senderMailAdderss"];
$result=mb_send_mail($mailto,$subject,$body,$mailfrom);
if($result){
	echo "レポートの送信が完了しました。";
}else{
	echo "レポートの送信に失敗しました。";
}
?>