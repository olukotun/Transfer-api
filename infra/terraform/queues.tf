# __generated__ by Terraform
# Please review these resources and move them into your main configuration files.

# __generated__ by Terraform from "https://sqs.us-east-2.amazonaws.com/611065450457/payments-transfer-requests"
resource "aws_sqs_queue" "transfer_requests" {
  content_based_deduplication       = false
  delay_seconds                     = 0
  fifo_queue                        = false
  kms_data_key_reuse_period_seconds = 300
  kms_master_key_id                 = null
  max_message_size                  = 1048576
  message_retention_seconds         = 345600
  name                              = "payments-transfer-requests"
  policy = jsonencode({
    Id = "__default_policy_ID"
    Statement = [{
      Action = "SQS:*"
      Effect = "Allow"
      Principal = {
        AWS = "arn:aws:iam::611065450457:root"
      }
      Resource = "arn:aws:sqs:us-east-2:611065450457:payments-transfer-requests"
      Sid      = "__owner_statement"
    }]
    Version = "2012-10-17"
  })
  receive_wait_time_seconds = 20
  redrive_policy = jsonencode({
    deadLetterTargetArn = "arn:aws:sqs:us-east-2:611065450457:payments-transfer-dlq"
    maxReceiveCount     = 5
  })
  region                     = "us-east-2"
  sqs_managed_sse_enabled    = true
  tags                       = {}
  tags_all                   = {}
  visibility_timeout_seconds = 30
}

# __generated__ by Terraform from "https://sqs.us-east-2.amazonaws.com/611065450457/payments-transfer-dlq"
resource "aws_sqs_queue" "transfer_dlq" {
  content_based_deduplication       = false
  delay_seconds                     = 0
  fifo_queue                        = false
  kms_data_key_reuse_period_seconds = 300
  kms_master_key_id                 = null
  max_message_size                  = 1048576
  message_retention_seconds         = 1209600
  name                              = "payments-transfer-dlq"
  policy = jsonencode({
    Id = "__default_policy_ID"
    Statement = [{
      Action = "SQS:*"
      Effect = "Allow"
      Principal = {
        AWS = "arn:aws:iam::611065450457:root"
      }
      Resource = "arn:aws:sqs:us-east-2:611065450457:payments-transfer-dlq"
      Sid      = "__owner_statement"
    }]
    Version = "2012-10-17"
  })
  receive_wait_time_seconds  = 0
  region                     = "us-east-2"
  sqs_managed_sse_enabled    = true
  tags                       = {}
  tags_all                   = {}
  visibility_timeout_seconds = 30
}
