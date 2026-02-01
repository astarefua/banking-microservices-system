from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from app.models.database import get_db
from app.models.schemas import (
    FraudCheckRequest,
    FraudCheckResponse,
    FraudAlertResponse
)
from app.services.fraud_service import FraudService
import logging

logger = logging.getLogger(__name__)

router = APIRouter()
fraud_service = FraudService()


@router.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "UP",
        "service": "fraud-detection-service",
        "message": "Fraud Detection Service is running"
    }


@router.post("/fraud/check", response_model=FraudCheckResponse)
async def check_fraud(
    request: FraudCheckRequest,
    db: Session = Depends(get_db)
):
    """
    Check a transaction for fraud
    """
    logger.info(f"REST API: Fraud check request for transaction: {request.transaction_id}")

    try:
        result = fraud_service.check_transaction(request, db)
        return result
    except Exception as e:
        logger.error(f"Error in fraud check: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/fraud/alerts", response_model=List[FraudAlertResponse])
async def get_all_alerts(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """
    Get all fraud alerts
    """
    logger.info(f"REST API: Getting all fraud alerts (skip={skip}, limit={limit})")

    try:
        alerts = fraud_service.get_fraud_alerts(db, skip, limit)
        return alerts
    except Exception as e:
        logger.error(f"Error getting alerts: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/fraud/alerts/account/{account_number}", response_model=List[FraudAlertResponse])
async def get_alerts_by_account(
    account_number: str,
    db: Session = Depends(get_db)
):
    """
    Get fraud alerts for a specific account
    """
    logger.info(f"REST API: Getting alerts for account: {account_number}")

    try:
        alerts = fraud_service.get_alerts_by_account(db, account_number)
        return alerts
    except Exception as e:
        logger.error(f"Error getting alerts for account: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/fraud/alerts/transaction/{transaction_id}", response_model=FraudAlertResponse)
async def get_alert_by_transaction(
    transaction_id: str,
    db: Session = Depends(get_db)
):
    """
    Get fraud alert for a specific transaction
    """
    logger.info(f"REST API: Getting alert for transaction: {transaction_id}")

    try:
        alert = fraud_service.get_alert_by_transaction(db, transaction_id)
        if not alert:
            raise HTTPException(status_code=404, detail="Alert not found")
        return alert
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting alert for transaction: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/fraud/stats")
async def get_fraud_stats(db: Session = Depends(get_db)):
    """
    Get fraud detection statistics
    """
    logger.info("REST API: Getting fraud statistics")

    try:
        from app.models.fraud_alert import FraudAlert, RiskLevel

        total_alerts = db.query(FraudAlert).count()
        critical_alerts = db.query(FraudAlert).filter(
            FraudAlert.risk_level == RiskLevel.CRITICAL
        ).count()
        high_alerts = db.query(FraudAlert).filter(
            FraudAlert.risk_level == RiskLevel.HIGH
        ).count()
        blocked_transactions = db.query(FraudAlert).filter(
            FraudAlert.is_blocked == 1
        ).count()

        return {
            "total_alerts": total_alerts,
            "critical_alerts": critical_alerts,
            "high_alerts": high_alerts,
            "blocked_transactions": blocked_transactions,
            "risk_distribution": {
                "critical": critical_alerts,
                "high": high_alerts,
                "medium": total_alerts - critical_alerts - high_alerts
            }
        }
    except Exception as e:
        logger.error(f"Error getting stats: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))