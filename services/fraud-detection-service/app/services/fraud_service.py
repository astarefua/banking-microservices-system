from sqlalchemy.orm import Session
from app.models.fraud_alert import FraudAlert, RiskLevel
from app.models.schemas import FraudCheckRequest, FraudCheckResponse, TransactionEvent
from app.ml.fraud_detector import FraudDetector
import uuid
import logging
from typing import List

logger = logging.getLogger(__name__)


class FraudService:
    def __init__(self):
        self.fraud_detector = FraudDetector()

    def check_transaction(
        self,
        request: FraudCheckRequest,
        db: Session
    ) -> FraudCheckResponse:
        """Check a transaction for fraud"""
        logger.info(f"Checking transaction: {request.transaction_id}")

        # Calculate risk score using ML model
        risk_score, reason, risk_level = self.fraud_detector.calculate_risk_score(
            amount=request.amount,
            transaction_type=request.transaction_type,
            account_number=request.account_number,
            to_account=request.to_account
        )

        # Determine if transaction is fraudulent
        is_fraud = risk_score >= 0.7

        # Get recommendations
        recommendations = self.fraud_detector.get_recommendations(risk_score, risk_level)

        # If high risk, create fraud alert
        if risk_level in [RiskLevel.HIGH, RiskLevel.CRITICAL]:
            self._create_fraud_alert(
                db=db,
                transaction_id=request.transaction_id,
                account_number=request.account_number,
                amount=request.amount,
                transaction_type=request.transaction_type,
                risk_score=risk_score,
                risk_level=risk_level,
                reason=reason,
                should_block=risk_level == RiskLevel.CRITICAL
            )

        return FraudCheckResponse(
            transaction_id=request.transaction_id,
            risk_score=round(risk_score, 3),
            risk_level=risk_level,
            is_fraud=is_fraud,
            reason=reason,
            recommendations=recommendations
        )

    def handle_transaction_event(self, event: TransactionEvent, db: Session):
        """Handle incoming transaction event from Kafka"""
        logger.info(f"Processing transaction event: {event.transactionId}")

        request = FraudCheckRequest(
            transaction_id=event.transactionId,
            account_number=event.fromAccount,
            amount=event.amount,
            transaction_type=event.type,
            to_account=event.toAccount
        )

        result = self.check_transaction(request, db)

        logger.info(
            f"Fraud check complete: {event.transactionId} - "
            f"Risk: {result.risk_score:.2f} ({result.risk_level})"
        )

        return result

    def _create_fraud_alert(
        self,
        db: Session,
        transaction_id: str,
        account_number: str,
        amount: float,
        transaction_type: str,
        risk_score: float,
        risk_level: RiskLevel,
        reason: str,
        should_block: bool
    ):
        """Create a fraud alert in the database"""
        alert_id = str(uuid.uuid4())

        fraud_alert = FraudAlert(
            alert_id=alert_id,
            transaction_id=transaction_id,
            account_number=account_number,
            risk_score=risk_score,
            risk_level=risk_level,
            reason=reason,
            amount=amount,
            transaction_type=transaction_type,
            is_blocked=1 if should_block else 0
        )

        db.add(fraud_alert)
        db.commit()
        db.refresh(fraud_alert)

        logger.warning(
            f"🚨 FRAUD ALERT CREATED: {alert_id} - "
            f"Transaction: {transaction_id}, Risk: {risk_score:.2f}, "
            f"Blocked: {should_block}"
        )

        return fraud_alert

    def get_fraud_alerts(self, db: Session, skip: int = 0, limit: int = 100) -> List[FraudAlert]:
        """Get all fraud alerts"""
        return db.query(FraudAlert).offset(skip).limit(limit).all()

    def get_alerts_by_account(self, db: Session, account_number: str) -> List[FraudAlert]:
        """Get fraud alerts for a specific account"""
        return db.query(FraudAlert).filter(
            FraudAlert.account_number == account_number
        ).all()

    def get_alert_by_transaction(self, db: Session, transaction_id: str) -> FraudAlert:
        """Get fraud alert for a specific transaction"""
        return db.query(FraudAlert).filter(
            FraudAlert.transaction_id == transaction_id
        ).first()