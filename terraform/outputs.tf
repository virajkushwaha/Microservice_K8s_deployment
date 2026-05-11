output "ssh_key_path" {
  value = "${path.module}/${var.key_pair_name}.pem"
}

output "ssh_master" {
  value = "ssh -i ${var.key_pair_name}.pem ubuntu@${aws_instance.master.public_ip}"
}

output "master_public_ip" {
  value = aws_instance.master.public_ip
}

output "frontend_url" {
  value = "http://${aws_instance.master.public_ip}:30400"
}

output "api_gateway_url" {
  value = "http://${aws_instance.master.public_ip}:30080"
}
