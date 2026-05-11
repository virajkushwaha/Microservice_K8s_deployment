variable "aws_region" {
  default = "us-east-1"
}

variable "key_pair_name" {
  default = "k8s-hotel-key"
}

# Ubuntu 22.04 LTS AMI for us-east-1
variable "ami_id" {
  default = "ami-0c7217cdde317cfec"
}

variable "instance_type" {
  default = "t3.medium"
}
