import {
  to = aws_sqs_queue.transfer_requests
  id = "https://sqs.us-east-2.amazonaws.com/611065450457/payments-transfer-requests"
}

import {
  to = aws_sqs_queue.transfer_dlq
  id = "https://sqs.us-east-2.amazonaws.com/611065450457/payments-transfer-dlq"
}
