resource "aws_iam_role" "k8s_role" {
  name = "k8s-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "ssm_s3_policy" {
  name = "k8s-ssm-s3-policy"
  role = aws_iam_role.k8s_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:PutParameter",
          "ssm:GetParameter",
          "ssm:DeleteParameter"
        ]
        Resource = "arn:aws:ssm:${var.aws_region}:*:parameter/k8s/*"
      },
      {
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:ListBucket"]
        Resource = [
          "arn:aws:s3:::${aws_s3_bucket.k8s_manifests.bucket}",
          "arn:aws:s3:::${aws_s3_bucket.k8s_manifests.bucket}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "k8s_profile" {
  name = "k8s-ssm-profile"
  role = aws_iam_role.k8s_role.name
}
