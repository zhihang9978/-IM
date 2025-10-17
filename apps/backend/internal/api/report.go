package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
)

type ReportHandler struct {
	reportDAO  *dao.ReportDAO
	messageDAO *dao.MessageDAO
	logDAO     *dao.OperationLogDAO
}

func NewReportHandler() *ReportHandler {
	return &ReportHandler{
		reportDAO:  dao.NewReportDAO(),
		messageDAO: dao.NewMessageDAO(),
		logDAO:     dao.NewOperationLogDAO(),
	}
}

// ReportMessage 举报消息
// POST /messages/report
// Body: {"message_id": 123, "reason": "垃圾营销"}
func (h *ReportHandler) ReportMessage(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	var req struct {
		MessageID uint   `json:"message_id" binding:"required"`
		Reason    string `json:"reason" binding:"required"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}
	
	// 验证举报原因
	validReasons := map[string]bool{
		"垃圾营销": true,
		"淫秽色情": true,
		"违法违规": true,
		"欺诈骗钱": true,
		"其他":   true,
	}
	
	if !validReasons[req.Reason] {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid report reason",
			"data":    nil,
		})
		return
	}
	
	// 检查消息是否存在
	message, err := h.messageDAO.GetByID(req.MessageID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Message not found",
			"data":    nil,
		})
		return
	}
	
	// 检查是否已举报过
	if h.reportDAO.CheckExists(userID, req.MessageID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Message already reported",
			"data":    nil,
		})
		return
	}
	
	// 创建举报记录
	report := &model.Report{
		ReporterID: userID,
		MessageID:  req.MessageID,
		Reason:     req.Reason,
		Status:     model.ReportStatusPending,
	}
	
	if err := h.reportDAO.Create(report); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to report message",
			"data":    nil,
		})
		return
	}
	
	// ✅ 记录操作日志
	h.logDAO.CreateLog(dao.LogRequest{
		Action: "message_report",
		UserID: &userID,
		IP:     c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"message_id": req.MessageID,
			"reason":     req.Reason,
		},
		Result: model.ResultSuccess,
	})
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Report submitted successfully",
		"data": gin.H{
			"report": report,
		},
	})
}

// GetReports 获取举报列表（用户自己的举报）
// GET /reports
func (h *ReportHandler) GetReports(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	
	reports, total, err := h.reportDAO.GetUserReports(userID, page, pageSize)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":     total,
			"page":      page,
			"page_size": pageSize,
			"reports":   reports,
		},
	})
}

// GetAllReports 获取所有举报记录（管理员）
// GET /admin/reports
func (h *ReportHandler) GetAllReports(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	status := c.Query("status")
	
	reports, total, err := h.reportDAO.GetAllReports(page, pageSize, status)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":     total,
			"page":      page,
			"page_size": pageSize,
			"reports":   reports,
		},
	})
}

// UpdateReportStatus 更新举报状态（管理员）
// PUT /admin/reports/:id
func (h *ReportHandler) UpdateReportStatus(c *gin.Context) {
	reportID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid report ID",
			"data":    nil,
		})
		return
	}
	
	var req struct {
		Status    string `json:"status" binding:"required"`
		AdminNote string `json:"admin_note"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}
	
	if err := h.reportDAO.UpdateStatus(uint(reportID), req.Status, req.AdminNote); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update report status",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Report status updated successfully",
		"data":    nil,
	})
}

