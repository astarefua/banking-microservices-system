import numpy as np
from typing import Dict, List, Tuple
from app.models.fraud_alert import RiskLevel
import logging

logger = logging.getLogger(__name__)


class FraudDetector:
    """
    ML-based fraud detection system.
    In production, this would use a trained ML model.
    For demo purposes, we use a rule-based system with weighted scoring.
    """

    def __init__(self):
        # Fraud detection thresholds
        self.high_risk_amount = 10000.0
        self.critical_amount = 50000.0
        self.velocity_threshold = 5  # Max transactions per hour

    def calculate_risk_score(
        self,
        amount: float,
        transaction_type: str,
        account_number: str,
        to_account: str = None
    ) -> Tuple[float, str, RiskLevel]:
        """
        Calculate fraud risk score (0.0 to 1.0)
        Returns: (risk_score, reason, risk_level)
        """
        risk_factors = []
        risk_score = 0.0

        # Factor 1: Transaction Amount (40% weight)
        amount_risk = self._check_amount_risk(amount)
        risk_score += amount_risk * 0.4
        if amount_risk > 0.5:
            risk_factors.append(f"High transaction amount: ${amount:,.2f}")

        # Factor 2: Transaction Type (20% weight)
        type_risk = self._check_transaction_type_risk(transaction_type)
        risk_score += type_risk * 0.2
        if type_risk > 0.5:
            risk_factors.append(f"Suspicious transaction type: {transaction_type}")

        # Factor 3: Round Number Detection (15% weight)
        round_number_risk = self._check_round_number(amount)
        risk_score += round_number_risk * 0.15
        if round_number_risk > 0.5:
            risk_factors.append("Transaction is a suspiciously round number")

        # Factor 4: Account Pattern (25% weight)
        # In production, this would check historical patterns
        pattern_risk = self._check_account_pattern(account_number, amount)
        risk_score += pattern_risk * 0.25
        if pattern_risk > 0.5:
            risk_factors.append("Unusual pattern for this account")

        # Determine risk level
        risk_level = self._determine_risk_level(risk_score)

        # Create reason message
        if not risk_factors:
            reason = "Transaction appears normal"
        else:
            reason = "; ".join(risk_factors)

        logger.info(f"Risk Score: {risk_score:.2f}, Level: {risk_level}, Factors: {len(risk_factors)}")

        return risk_score, reason, risk_level

    def _check_amount_risk(self, amount: float) -> float:
        """Check risk based on transaction amount"""
        if amount >= self.critical_amount:
            return 1.0
        elif amount >= self.high_risk_amount:
            return 0.7
        elif amount >= 5000:
            return 0.4
        elif amount >= 1000:
            return 0.2
        else:
            return 0.0

    def _check_transaction_type_risk(self, transaction_type: str) -> float:
        """Check risk based on transaction type"""
        # WITHDRAWAL and TRANSFER are riskier than DEPOSIT
        risk_map = {
            "WITHDRAWAL": 0.6,
            "TRANSFER": 0.5,
            "DEPOSIT": 0.1
        }
        return risk_map.get(transaction_type.upper(), 0.3)

    def _check_round_number(self, amount: float) -> float:
        """Fraudsters often use round numbers"""
        # Check if amount is a round number (e.g., 5000, 10000)
        if amount % 1000 == 0 and amount >= 5000:
            return 0.6
        elif amount % 500 == 0 and amount >= 2000:
            return 0.4
        return 0.0

    def _check_account_pattern(self, account_number: str, amount: float) -> float:
        """
        Check account patterns.
        In production, this would analyze historical data.
        For demo, we use simple heuristics.
        """
        # Simulate: New accounts (shorter account numbers) are riskier
        if len(account_number) < 8:
            return 0.5

        # Simulate: Certain account patterns
        if account_number.startswith("999"):
            return 0.7

        return 0.0

    def _determine_risk_level(self, risk_score: float) -> RiskLevel:
        """Determine risk level from score"""
        if risk_score >= 0.8:
            return RiskLevel.CRITICAL
        elif risk_score >= 0.6:
            return RiskLevel.HIGH
        elif risk_score >= 0.3:
            return RiskLevel.MEDIUM
        else:
            return RiskLevel.LOW

    def get_recommendations(self, risk_score: float, risk_level: RiskLevel) -> List[str]:
        """Get fraud prevention recommendations"""
        recommendations = []

        if risk_level == RiskLevel.CRITICAL:
            recommendations.extend([
                "BLOCK transaction immediately",
                "Contact account holder for verification",
                "Flag account for investigation",
                "Review recent transaction history"
            ])
        elif risk_level == RiskLevel.HIGH:
            recommendations.extend([
                "Require additional authentication",
                "Send verification SMS/Email",
                "Review transaction manually",
                "Monitor account closely"
            ])
        elif risk_level == RiskLevel.MEDIUM:
            recommendations.extend([
                "Send notification to account holder",
                "Log for future pattern analysis",
                "Consider velocity checks"
            ])
        else:
            recommendations.append("No action required - proceed normally")

        return recommendations