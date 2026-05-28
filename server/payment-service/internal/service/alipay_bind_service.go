package service

import (
	"context"
	"errors"
	"strings"
	"time"

	"campusmart/payment-service/internal/model"
	"campusmart/payment-service/internal/repository"
)

var ErrInvalidAlipayUserID = errors.New("alipayUserId不能为空")

type BindAlipayRequest struct {
	AlipayUserID  string `json:"alipayUserId"`
	AlipayLoginID string `json:"alipayLoginId"`
	Nickname      string `json:"nickname"`
}

type AlipayBindService struct {
	repo *repository.AlipayBindRepository
}

func NewAlipayBindService(repo *repository.AlipayBindRepository) *AlipayBindService {
	return &AlipayBindService{repo: repo}
}

func (s *AlipayBindService) BindMock(ctx context.Context, userID int64, req BindAlipayRequest) (*model.AlipayBindResponse, error) {
	alipayUserID := strings.TrimSpace(req.AlipayUserID)
	if alipayUserID == "" {
		return nil, ErrInvalidAlipayUserID
	}

	bind := &model.AlipayAccountBind{
		UserID:        userID,
		AlipayUserID:  alipayUserID,
		AlipayLoginID: strings.TrimSpace(req.AlipayLoginID),
		Nickname:      strings.TrimSpace(req.Nickname),
		BindTime:      time.Now(),
		IsDeleted:     0,
	}
	if err := s.repo.Upsert(ctx, bind); err != nil {
		return nil, err
	}
	return toResponse(bind), nil
}

func (s *AlipayBindService) GetBind(ctx context.Context, userID int64) (*model.AlipayBindResponse, error) {
	bind, err := s.repo.FindByUserID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if bind == nil {
		return &model.AlipayBindResponse{Bound: false}, nil
	}
	return toResponse(bind), nil
}

func (s *AlipayBindService) Unbind(ctx context.Context, userID int64) error {
	return s.repo.SoftDeleteByUserID(ctx, userID)
}

func toResponse(bind *model.AlipayAccountBind) *model.AlipayBindResponse {
	return &model.AlipayBindResponse{
		Bound:         true,
		AlipayUserID:  bind.AlipayUserID,
		AlipayLoginID: bind.AlipayLoginID,
		Nickname:      bind.Nickname,
	}
}
