# __generated__ by Terraform
# Please review these resources and move them into your main configuration files.

# __generated__ by Terraform
resource "aws_sns_topic" "payments_alerts" {
  application_failure_feedback_role_arn    = null
  application_success_feedback_role_arn    = null
  application_success_feedback_sample_rate = 0
  archive_policy                           = null
  content_based_deduplication              = false
  delivery_policy                          = null
  display_name                             = null
  fifo_topic                               = false
  firehose_failure_feedback_role_arn       = null
  firehose_success_feedback_role_arn       = null
  firehose_success_feedback_sample_rate    = 0
  http_failure_feedback_role_arn           = null
  http_success_feedback_role_arn           = null
  http_success_feedback_sample_rate        = 0
  kms_master_key_id                        = null
  lambda_failure_feedback_role_arn         = null
  lambda_success_feedback_role_arn         = null
  lambda_success_feedback_sample_rate      = 0
  name                                     = "payments-alerts"
  policy = jsonencode({
    Id = "__default_policy_ID"
    Statement = [{
      Action = ["SNS:GetTopicAttributes", "SNS:SetTopicAttributes", "SNS:AddPermission", "SNS:RemovePermission", "SNS:DeleteTopic", "SNS:Subscribe", "SNS:ListSubscriptionsByTopic", "SNS:Publish"]
      Condition = {
        StringEquals = {
          "AWS:SourceOwner" = "611065450457"
        }
      }
      Effect = "Allow"
      Principal = {
        AWS = "*"
      }
      Resource = "arn:aws:sns:us-east-2:611065450457:payments-alerts"
      Sid      = "__default_statement_ID"
    }]
    Version = "2008-10-17"
  })
  region                           = "us-east-2"
  signature_version                = 1
  sqs_failure_feedback_role_arn    = null
  sqs_success_feedback_role_arn    = null
  sqs_success_feedback_sample_rate = 0
  tags                             = {}
  tags_all                         = {}
}

# __generated__ by Terraform
resource "aws_cloudwatch_metric_alarm" "transfer_dlq_not_empty" {
  actions_enabled     = true
  alarm_actions       = [aws_sns_topic.payments_alerts.arn]
  alarm_description   = null
  alarm_name          = "payments-transfer-dlq-not-empty"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = 1
  dimensions = {
    QueueName = aws_sqs_queue.transfer_dlq.name
  }
  evaluation_periods        = 1
  extended_statistic        = null
  insufficient_data_actions = []
  metric_name               = "ApproximateNumberOfMessagesVisible"
  namespace                 = "AWS/SQS"
  ok_actions                = []
  period                    = 60
  region                    = "us-east-2"
  statistic                 = "Maximum"
  tags                      = {}
  tags_all                  = {}
  threshold                 = 1
  threshold_metric_id       = null
  treat_missing_data        = "notBreaching"
  unit                      = null
}
